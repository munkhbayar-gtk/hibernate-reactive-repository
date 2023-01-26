package io.github.mbr.hibernate.reactive.impl;

import io.github.mbr.hibernate.reactive.data.jpql.Query;
import io.github.mbr.hibernate.reactive.impl.annotations.RepositoryMethod;
import io.github.mbr.hibernate.reactive.impl.annotations.RepositoryPagedMethod;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.uni.UniReactorConverters;
import lombok.extern.slf4j.Slf4j;
import io.github.mbr.hibernate.reactive.ReactivePersistentUnitInfo;
import io.github.mbr.hibernate.reactive.config.EntityMetaData;
import io.github.mbr.hibernate.reactive.config.RepoInterfaceMetaData;
import org.hibernate.reactive.mutiny.Mutiny;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.core.EntityMetadata;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.criteria.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

import static io.smallrye.mutiny.converters.uni.UniReactorConverters.*;
@Slf4j

public class _JQL_MethodExecutorImpl implements RepoInterfaceMetaData.IMethodInvokers {

     public static _JQL_MethodExecutorImpl of(ReactivePersistentUnitInfo config) {
          return new _JQL_MethodExecutorImpl(config);
     }
     private ReactivePersistentUnitInfo persistentUnitInfo;
     private Map<String, _JQL_MethodImpl> impls = new HashMap<>();
     private Map<String, _JQL_MethodImpl> implsOfPagedMethods = new HashMap<>();
     //private MethodGraph methodGraph = new MethodGraph();

     private _JQL_MethodExecutorImpl(ReactivePersistentUnitInfo persistentUnitInfo) {
          this.persistentUnitInfo = persistentUnitInfo;

          impls.put("findAll", this::findAll);
          impls.put("findAllById", this::findAllById);
          impls.put("findById", this::findById);
          impls.put("save", this::save);
          impls.put("saveAll", this::saveAll);
          impls.put("deleteById", this::deleteById);
          impls.put("deleteAllById", this::deleteAllById);
          impls.put("deleteAll", this::deleteAll );
          impls.put("getMutinySessionFactory", this::getMutinySessionFactory);
          impls.put("getStageSessionFactory", this::getStageSessionFactory);

          implsOfPagedMethods.put("findAll", this::findAllPaged);
          implsOfPagedMethods.put("findAllById", this::findAllByIdPaged);

     }

     private String getCountQuery(String jpql) {
          return "SELECT COUNT(*) " + jpql;
     }
     private String getOrderQuery(String jpql, Sort sort) {
          List<Sort.Order> orders = sort.get().collect(Collectors.toList());
          if(orders.isEmpty()) return jpql;

          StringBuilder sb = new StringBuilder();
          sb.append(jpql).append(" ORDER BY ");
          orders.forEach(order -> {
               sb.append(order.getProperty()).append(" ").append(order.getDirection().name());
          });
          return sb.substring(0, sb.length() - 1);
     }
     private boolean isPaged(Object [] args) {
          if(args == null || args.length == 0) return false;
          return args[args.length - 1] instanceof Pageable;
     }
     private Object executeAndReturnList(RepoInterfaceMetaData.QueryMethodMetaData metaData, String jpql, Object [] args){
          Mutiny.SessionFactory sf = sessionFactory();
          boolean paged =isPaged(args);
          if(paged) {

               Map<String, Object> bindings = metaData.getParamsBindings(args);

               Uni<Long> countUni = sf.withSession((session->{
                    String countQuery = getCountQuery(jpql);
                    Mutiny.Query<Long> query = createQuery(session, bindings, countQuery);
                    return query.getSingleResult();
               }));
               Pageable pageable = (Pageable)args[args.length - 1];

               Uni<?> resultUni = sf.withSession(session -> {
                    Mutiny.Query<?> query = createQuery(session, bindings, getOrderQuery(jpql, pageable.getSort()));
                    query
                            .setFirstResult(pageable.getPageNumber() * pageable.getPageSize())
                            .setMaxResults(pageable.getPageSize());

                    return query.getResultList();
               });

               return Mono.zip(to_Mono(countUni), to_Mono(resultUni)).flatMap(tuple->{
                    long count = tuple.getT1();
                    List<?> list = (List<?>) tuple.getT2();
                    return Mono.just(new PageImpl<>(list, pageable, count));
               });

          }

          Uni<?> resultUni = sf.withSession(session -> {
               Mutiny.Query<?> query = createQuery(session, metaData, jpql, args);
               return query.getResultList();
          });
          if(metaData.isReturningFlux()){
               return to_FLux(resultUni);
          }
          return to_Mono(resultUni);
     }

     private <T>Mutiny.Query<T> createQuery(Mutiny.Session session, Map<String, Object> bindings, String jpql) {
          Mutiny.Query<T> query = session.createQuery(jpql);
          bindings.forEach(query::setParameter);
          return query;
     }

     private <T>Mutiny.Query<T> createQuery(Mutiny.Session session, RepoInterfaceMetaData.QueryMethodMetaData metaData, String jpql, Object [] args) {
          Map<String, Object> bindings = metaData.getParamsBindings(args);
          return createQuery(session, bindings, jpql);
          /*
          String [] params = metaData.getParams();
          for (int i = 0; i < params.length; i++) {
               Object param = args[i];
               query.setParameter(params[i], param);
          }
          return query;
           */
     }
     private Object executeQuery(RepoInterfaceMetaData repoInterfaceMetaData, Object proxy, Method method, Object [] args){

          RepoInterfaceMetaData.QueryMethodMetaData metaData = repoInterfaceMetaData.getQueryMethodMetaData(method);
          String jpql = metaData.getQuery();
          log.debug("SQL: {}", jpql);
          if(metaData.isResultList()){
               return executeAndReturnList(metaData, jpql, args);
          }
          Mutiny.SessionFactory sf = sessionFactory();
          Uni<?> uni = sf.withSession(session -> {
               Mutiny.Query<?> query = createQuery(session, metaData, jpql, args);
               Uni<?> retval;
               if(metaData.isSingleResult()) {
                    retval = query.getSingleResult();
               }else {
                    retval = query.executeUpdate();
               }
               return retval;
             //query.se
          });
          if(metaData.isSingleResult()){
               return to_Mono(uni);
          }
          return to_Mono(uni).flatMap(obj->Mono.empty());
     }

     private Mono<Page<?>> findAllPaged(Object[] args, Class<?> entityClass, Class<?> idClass) {
          Pageable pageable = (Pageable) args[0];
          /*
          Mutiny.SessionFactory sf = sessionFactory();
          CriteriaBuilder cb = sf.getCriteriaBuilder();
          CriteriaQuery<?> q = cb.createQuery(entityClass);
          //Root<?> root =
          q.from(entityClass);
          return sf.withSession(session -> session.createQuery(q).getResultList());
           */
          Q q = createQuery(entityClass);
          CriteriaQuery<?> query = q.q;
          CriteriaQuery<Long> countQuery = q.cb.createQuery(Long.class);
          countQuery.select(q.cb.count(countQuery.from(entityClass)));

          return getPagedResult(q,countQuery, query, pageable);

     }

     private Order order(CriteriaBuilder cb, Root<?> root, Sort.Order order) {
          if(order.isAscending()) {
               return cb.asc(root.get(order.getProperty()));
          }
          return cb.desc(root.get(order.getProperty()));
     }

     private Mono<Page<?>> _executeListResult(long count, Q q, CriteriaQuery<?> selectQuery, Pageable pageable){
          Sort sort = pageable.getSort();
          List<Order> orders = sort.get().map(order -> order(q.cb, q.root, order)).collect(Collectors.toList());
          selectQuery.orderBy(orders);

          Mono<?> resultMono = to_Mono(q.executeWithSession(session -> session
                  .createQuery(selectQuery)
                  .setFirstResult(pageable.getPageNumber() * pageable.getPageSize())
                  .setMaxResults(pageable.getPageSize())
                  .getResultList()));

          return resultMono.flatMap((listObj)->{
               List<?> list = (List<?>)listObj;
               return Mono.just(new PageImpl<>(list, pageable, count));
          });

     }
     private Mono<Page<?>> getPagedResult(Q q, CriteriaQuery<Long> countQuery, CriteriaQuery<?> selectQuery, Pageable pageable) {
          Mono<Long> countMono = to_Mono(
                  q.sf.withSession((session -> session
                          .createQuery(countQuery)
                          .getSingleResult()))
          );

          return countMono.flatMap((count)->{
               if(count > 0) {
                    return _executeListResult(count, q, selectQuery, pageable);
               }
               return Mono.just(Page.empty());
          });

          /*
          Sort sort = pageable.getSort();
          List<Order> orders = sort.get().map(order -> order(q.cb, q.root, order)).collect(Collectors.toList());
          selectQuery.orderBy(orders);

          Mono<?> resultMono = to_Mono(q.executeWithSession(session -> session
                  .createQuery(selectQuery)
                  .setFirstResult(pageable.getPageNumber() * pageable.getPageSize())
                  .setMaxResults(pageable.getPageSize())
                  .getResultList()));

          return Mono.zip(countMono,resultMono).flatMap(tuple->{
               long count = tuple.getT1();
               List<?> list = (List<?>) tuple.getT2();
               return Mono.just(new PageImpl<>(list, pageable, count));
          });

           */
     }

     private Mono<Page<?>> findAllByIdPaged(Object[] args, Class<?> entityClass, Class<?> idClass) {

          Pageable pageable = (Pageable) args[1];

          Q q = createQuery(entityClass);
          //delete.where(cb.equal(root.get("C_ID"), user.getId()));

          List<Object> idList = new ArrayList<>();
          Iterable<?> ids = (Iterable<?>) args[0];
          ids.forEach(idList::add);

          String idColName = getIdColumnName(entityClass);
          CriteriaQuery<?> query = q.q;
          Predicate predicate = q.root.get(idColName).in(idList);
          query.where(predicate);

          CriteriaQuery<Long> countQuery = q.cb.createQuery(Long.class);
          countQuery.select(q.cb.count(countQuery.from(entityClass))).where(predicate);

          return getPagedResult(q,countQuery,query,pageable);
     }

     private Flux<?> findAll(Object[] args, Class<?> entityClass, Class<?> idClass) {
          /*
          Mutiny.SessionFactory sf = sessionFactory();
          CriteriaBuilder cb = sf.getCriteriaBuilder();
          CriteriaQuery<?> q = cb.createQuery(entityClass);
          //Root<?> root =
          q.from(entityClass);
          return sf.withSession(session -> session.createQuery(q).getResultList());
           */
          Q q = createQuery(entityClass);
          return to_FLux(
                  q.executeWithSession((session -> session.createQuery(q.q).getResultList()))
          );
     }
     private Flux<?> findAllById(Object[] args, Class<?> entityClass, Class<?> idClass) {
          Q q = createQuery(entityClass);
          //delete.where(cb.equal(root.get("C_ID"), user.getId()));

          List<Object> idList = new ArrayList<>();
          Iterable<?> ids = (Iterable<?>) args[0];
          ids.forEach(idList::add);

          String idColName = getIdColumnName(entityClass);
          CriteriaQuery<?> query = q.q;
          query.where(q.root.get(idColName).in(idList));
          return to_FLux(
                  q.executeWithSession((session -> session.createQuery(query).getResultList()))
          );
     }



     private Mono<?> findById(Object[] args, Class<?> entityClass, Class<?> idClass) {
          Q q = createQuery(entityClass);
          //delete.where(cb.equal(root.get("C_ID"), user.getId()));
          String idColName = getIdColumnName(entityClass);
          CriteriaQuery<?> query = q.q;
          query.where(q.cb.equal(q.root.get(idColName), args[0]));
          return to_Mono(
                  q
                          .executeWithSession(
                                  (session -> session.createQuery(query).getSingleResult()))
          );
     }
     private Mono<?> save(Object[] args, Class<?> entityClass, Class<?> idClass) {
          Object entity = args[0];
          Mutiny.SessionFactory sf = sessionFactory();
          Object idVal = getIdValue(entity, entityClass);

          Uni<?> uni;
          if (idVal == null) {
               uni= sf.withSession(session -> session.persist(entity).chain(session::flush).replaceWith(entity));
          }
          uni = sessionFactory().withSession(session->session.merge(entity).onItem().call(session::flush));
          return to_Mono(uni);
     }

     private Flux<?> saveAll(Object[] args, Class<?> entityClass, Class<?> idClass) {

          List<Object> eList = new ArrayList<>();
          Iterable<?> entitiesIterator = (Iterable<?>) args[0];
          entitiesIterator.forEach(eList::add);

          Object[] entities = eList.toArray();
          Mutiny.SessionFactory sf = sessionFactory();
          return to_FLux(
                  sf.withSession(session -> session.persistAll(entities).chain(session::flush).replaceWith(entities))
          );
     }
     private Mono<?> deleteById(Object[] args, Class<?> entityClass, Class<?> idClass) {
          Object id = args[0];
          Q q = createDelete(entityClass);

          String idColName = getIdColumnName(entityClass);
          q.del.where(q.cb.equal(q.root.get(idColName), id));

          return to_Mono(
                  q.executeWithTransaction((session,tx) -> session.createQuery(q.del).executeUpdate())
          );
     }
     private Mono<?> deleteAllById(Object[] args, Class<?> entityClass, Class<?> idClass) {
          Q q = createDelete(entityClass);
          //delete.where(cb.equal(root.get("C_ID"), user.getId()));

          List<Object> idList = new ArrayList<>();
          Iterable<?> ids = (Iterable<?>) args[0];
          ids.forEach(idList::add);

          String idColName = getIdColumnName(entityClass);
          q.del.where(q.root.get(idColName).in(idList));

          return to_Mono(
                  q.executeWithTransaction((session, tx)->
                          session.createQuery(q.del).executeUpdate()
                  )
          );
     }
     private Mono<?> deleteAll(Object[] args, Class<?> entityClass, Class<?> idClass) {

          Q q = createDelete(entityClass);

          return to_Mono(
                  q.executeWithTransaction((session, tx)->session.createQuery(q.del).executeUpdate())
          );
     }
     private Object getMutinySessionFactory(Object[] args, Class<?> entityClass, Class<?> idClass) {
          return persistentUnitInfo.getMutinySessionFactory();
     }
     private Object getStageSessionFactory(Object[] args, Class<?> entityClass, Class<?> idClass) {
          return persistentUnitInfo.getStageSessionFactory();
     }

     private Mutiny.SessionFactory sessionFactory() {
          return persistentUnitInfo.getMutinySessionFactory();
     }

     private String getIdColumnName(Class<?> entityClass) {
          Field idField = getIdColField(entityClass);
          return getIdColumnName(idField);
     }

     private Object getIdValue(Object entity, Class<?> entityClass) {
          Field idField = getIdColField(entityClass);
          try{
               Method getter = getGetter(idField, entityClass);

               return getter.invoke(entity );
          }catch (Exception e){
               throw new RuntimeException(e);
          }
     }
     private Method getGetter(Field idField, Class<?> clazz) throws Exception{
          String nm = idField.getName();
          String prefix = "get";
          if(idField.getType().isAssignableFrom(boolean.class)) {
               prefix = "is";
          }
          String getterName = prefix + Character.toUpperCase(nm.charAt(0)) + nm.substring(1);
          return clazz.getMethod(getterName);
     }
     private Field getIdColField(Class<?> entityClass) {
          Field idField = findByAnnotation(entityClass.getFields(), Id.class);
          if(idField == null) {
               idField = findByAnnotation(entityClass.getDeclaredFields(), Id.class);
          }
          return idField;
     }
     private String getIdColumnName(Field idField) {
          Column idCol = idField.getAnnotation(Column.class);
          String colName = null;
          if(idCol == null){
               colName = idField.getName();
          }else{
               colName = idCol.name();
          }
          return colName;
     }
     private Field findByAnnotation(Field[] fields, Class<? extends Annotation> annotationClass) {
          for(Field f : fields) {

               Annotation anno = f.getAnnotation(annotationClass);
               if(anno != null) return f;

               anno = f.getDeclaredAnnotation(annotationClass);
               if(anno != null) return f;

               if(f.getAnnotatedType().isAnnotationPresent(annotationClass)){
                    return f;
               }
          }
          return null;
     }

     private <T> Q createDelete(Class<T> entityClass) {
          Mutiny.SessionFactory sf = sessionFactory();
          CriteriaBuilder cb = sf.getCriteriaBuilder();
          CriteriaDelete<T> delete = cb.createCriteriaDelete(entityClass);
          Root<T> root = delete.from(entityClass);

          Q ret = new Q();
          ret.sf = sf;
          ret.cb = cb;
          ret.root = root;
          ret.del = delete;
          return ret;
     }
     private Q createQuery(Class<?> entityClass){
          Mutiny.SessionFactory sf = sessionFactory();
          CriteriaBuilder cb = sf.getCriteriaBuilder();
          CriteriaQuery<?> q = cb.createQuery(entityClass);
          Root<?> root = q.from(entityClass);

          Q ret = new Q();
          ret.sf = sf;
          ret.cb = cb;
          ret.root = root;
          ret.q = q;
          return ret;
     }
     private <T>Mono<T> to_Mono(Uni<T> uni) {
          return uni.convert().with(toMono());
     }
     private <T>Flux<T> to_FLux(Uni<T> uni) {
          Mono<?> mono = uni.convert().with(UniReactorConverters.toMono());
          return mono.flatMapMany((item)->{
               List<T> list = (List<T>) item;
               return Flux.fromIterable(list);
          });
     }

     @Override
     public RepoInterfaceMetaData.IMethodInvoker getRepositoryMethodInvoker() {
          return (repoMetaData, proxy, method, args)->{
               EntityMetaData entityMetaData = repoMetaData.getEntityMetaData();
               return impls.get(method.getName()).execute(args, entityMetaData.entityClass, entityMetaData.idClass);
          };
     }

     @Override
     public RepoInterfaceMetaData.IMethodInvoker getRepositoryPagedMethodInvoker() {
          return (repoMetaData, proxy, method, args)->{
               EntityMetaData entityMetaData = repoMetaData.getEntityMetaData();
               return implsOfPagedMethods.get(method.getName()).execute(args, entityMetaData.entityClass, entityMetaData.idClass);
          };
     }

     @Override
     public RepoInterfaceMetaData.IMethodInvoker getQueryInvoker() {
          return this::executeQuery;
     }

     @Override
     public RepoInterfaceMetaData.IMethodInvoker getDefaultMethodInvoker() {
          return null;
     }

     /*
               RepositoryMethod repoMethodAnnotation = method.getAnnotation(RepositoryMethod.class);
               if(repoMethodAnnotation != null) {
                    return impls.get(name).execute(args, entityClass, idClass);
               }
               RepositoryPagedMethod repoPagedMethodAnnotation = method.getAnnotation(RepositoryPagedMethod.class);
               if(repoPagedMethodAnnotation != null) {
                    return implsOfPagedMethods.get(name).execute(args, entityClass, idClass);
               }

               Query query = method.getAnnotation(Query.class);
               if(query != null) {
                    return executeQuery(metaData, method, args);
               }

               //Queried
               log.debug("method is default: {}", method.isDefault());
               throw new RuntimeException(method.getName() + " is not executable, is default: " + method.isDefault());
      */
     private static class Q {
          Mutiny.SessionFactory sf;
          CriteriaBuilder cb;
          CriteriaQuery<?> q;
          CriteriaDelete<?> del;
          Root<?> root;
          Uni<?> executeWithSession(QExecute execute) {
               return sf.withSession(execute::exec);
          }
          Uni<?> executeWithTransaction(TxExecute execute) {
               return sf.withTransaction(execute::exec);
          }
     }
     private interface QExecute {
          Uni<?> exec(Mutiny.Session session);
     }
     private interface TxExecute {
          Uni<?> exec(Mutiny.Session session, Mutiny.Transaction tx);
     }

}
