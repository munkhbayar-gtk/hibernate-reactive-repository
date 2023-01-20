package mbr.hibernate.reactive.impl;

import io.smallrye.mutiny.Uni;
import lombok.extern.slf4j.Slf4j;
import mbr.hibernate.reactive.ReactivePersistentUnitInfo;
import org.hibernate.reactive.mutiny.Mutiny;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

@Slf4j

public class _JQL_MethodExecutorImpl {

     public static _JQL_MethodExecutorImpl of(ReactivePersistentUnitInfo config) {
          return new _JQL_MethodExecutorImpl(config);
     }
     private ReactivePersistentUnitInfo persistentUnitInfo;
     private Map<String, _JQL_MethodImpl> impls = new HashMap<>();

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

     }
     public final Object execute(Class<?> repoInterface, Method method, Object[] args) {
          //method.getDeclaringClass().getGenericSuperclass()

          Class<?> entityClass = findEntityClass(repoInterface,method);
          Class<?> idClass = findIdClass(repoInterface,method);
          String name = method.getName();
          return impls.get(name).execute(args, entityClass, idClass);
     };

     private ParameterizedType find(Class<?> repoInterface, Method method) {
          log.debug("method: {}", method.getName());
          Class<?> repoClass = method.getDeclaringClass();
          log.debug("method-class: {}", method.getDeclaringClass());
          log.debug("method-obj: {}", repoInterface);
          ParameterizedType parameterizedTypes = extractParameterizedTypes(repoInterface);
          log.debug("method-param-types: {}", parameterizedTypes);
          //AnnotatedType superType = repoClass.getAnnotatedInterfaces()[0];
          //return (ParameterizedType) superType.getType();
          //return (ParameterizedType)parameterizedTypes.get(0);
          return parameterizedTypes;
     }
     private ParameterizedType extractParameterizedTypes(Class<?> repoInterface) {

          Type[] parameterizedTypes = repoInterface.getGenericInterfaces();
          for (Type parameterizedType : parameterizedTypes) {
               if(parameterizedType instanceof ParameterizedType) {
                    return ((ParameterizedType)parameterizedType);
                    /*
                    Type[] actualTypeArgs = ((ParameterizedType)parameterizedType).getActualTypeArguments();
                    ret.addAll(Arrays.asList(actualTypeArgs));
                     */
               }
          }
          return null;
     }
     private Class<?> findEntityClass(Class<?> repoInterface, Method method) {
          return (Class<?>)find(repoInterface, method).getActualTypeArguments()[0];//.getClass();
     }
     private Class<?> findIdClass(Class<?> repoInterface, Method method) {
          return (Class<?>)find(repoInterface, method).getActualTypeArguments()[1];//.getClass();
     }


     private Object findAll(Object[] args, Class<?> entityClass, Class<?> idClass) {

          /*
          Mutiny.SessionFactory sf = sessionFactory();
          CriteriaBuilder cb = sf.getCriteriaBuilder();
          CriteriaQuery<?> q = cb.createQuery(entityClass);
          //Root<?> root =
          q.from(entityClass);
          return sf.withSession(session -> session.createQuery(q).getResultList());
           */
          Q q = createQuery(entityClass);
          return q.executeWithSession((session -> session.createQuery(q.q).getResultList()));
     }
     private Object findAllById(Object[] args, Class<?> entityClass, Class<?> idClass) {
          Q q = createQuery(entityClass);
          //delete.where(cb.equal(root.get("C_ID"), user.getId()));

          List<Object> idList = new ArrayList<>();
          Iterable<?> ids = (Iterable<?>) args[0];
          ids.forEach(idList::add);

          String idColName = getIdColumnName(entityClass);
          CriteriaQuery<?> query = q.q;
          query.where(q.root.get(idColName).in(idList));

          return q.executeWithSession((session -> session.createQuery(query).getResultList()));
     }

     private Object findById(Object[] args, Class<?> entityClass, Class<?> idClass) {
          Q q = createQuery(entityClass);
          //delete.where(cb.equal(root.get("C_ID"), user.getId()));
          String idColName = getIdColumnName(entityClass);
          CriteriaQuery<?> query = q.q;
          query.where(q.cb.equal(q.root.get(idColName), args[0]));
          return q.executeWithSession((session -> session.createQuery(query).getSingleResult()));
     }
     private Object save(Object[] args, Class<?> entityClass, Class<?> idClass) {
          Object entity = args[0];
          Mutiny.SessionFactory sf = sessionFactory();
          Object idVal = getIdValue(entity, entityClass);
          if (idVal == null) {
               return sf.withSession(session -> session.persist(entity).chain(session::flush).replaceWith(entity));
          }
          return sessionFactory().withSession(session->session.merge(entity).onItem().call(session::flush));
     }

     private Object saveAll(Object[] args, Class<?> entityClass, Class<?> idClass) {

          List<Object> eList = new ArrayList<>();
          Iterable<?> entitiesIterator = (Iterable<?>) args[0];
          entitiesIterator.forEach(eList::add);

          Object[] entities = eList.toArray();
          Mutiny.SessionFactory sf = sessionFactory();
          return sf.withSession(session -> session.persistAll(entities).chain(session::flush).replaceWith(entities));
     }
     private Object deleteById(Object[] args, Class<?> entityClass, Class<?> idClass) {
          Object id = args[0];
          Q q = createDelete(entityClass);

          String idColName = getIdColumnName(entityClass);
          q.del.where(q.cb.equal(q.root.get(idColName), id));

          return q.executeWithTransaction((session,tx) -> session.createQuery(q.del).executeUpdate());
     }
     private Object deleteAllById(Object[] args, Class<?> entityClass, Class<?> idClass) {
          Q q = createDelete(entityClass);
          //delete.where(cb.equal(root.get("C_ID"), user.getId()));

          List<Object> idList = new ArrayList<>();
          Iterable<?> ids = (Iterable<?>) args[0];
          ids.forEach(idList::add);

          String idColName = getIdColumnName(entityClass);
          q.del.where(q.root.get(idColName).in(idList));

          return q.executeWithTransaction((session, tx)->
               session.createQuery(q.del).executeUpdate()
          );
     }
     private Object deleteAll(Object[] args, Class<?> entityClass, Class<?> idClass) {

          Q q = createDelete(entityClass);

          return q.executeWithTransaction((session, tx)->session.createQuery(q.del).executeUpdate());
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
