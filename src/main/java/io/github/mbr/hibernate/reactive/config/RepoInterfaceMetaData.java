package io.github.mbr.hibernate.reactive.config;

import io.github.mbr.hibernate.reactive.ReactiveHibernateCrudRepository;
import io.github.mbr.hibernate.reactive.data.jpql.Param;
import io.github.mbr.hibernate.reactive.data.jpql.Query;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.persistence.Column;
import javax.persistence.Id;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j

public class RepoInterfaceMetaData {
    public final Class<? extends ReactiveHibernateCrudRepository<?,?>> repoInterfaceClass;
    private EntityMetaData entityMetaData;

    private Map<Method, QueryMethodMetaData> queryMethodMetaDataMap = new HashMap<>();

    public static RepoInterfaceMetaData of(Class<? extends ReactiveHibernateCrudRepository<?,?>> repoInterfaceClass) {
        return new RepoInterfaceMetaData(repoInterfaceClass);
    }
    private RepoInterfaceMetaData(Class<? extends ReactiveHibernateCrudRepository<?,?>> repoInterfaceClass) {
        this.repoInterfaceClass = repoInterfaceClass;
        buildEntityMetaData();
        buildQueryMethodMetaDatas();
    }

    private void buildEntityMetaData() {
        Class<?> entityClass = findEntityClass();
        Class<?> idClass = findIdClass();
        String idColName = getIdColumnName(entityClass);
        this.entityMetaData = new EntityMetaData(entityClass, idClass, idColName);
    }

    private void buildQueryMethodMetaDatas(){
        List<Method> methods = new ArrayList<>();
        methods.addAll(List.of(repoInterfaceClass.getMethods()));
        methods.addAll(List.of(repoInterfaceClass.getDeclaredMethods()));

        methods.forEach(method->{
            if(isQueryMethod(method)){
                QueryMethodMetaData metaData = createMetaData(method);
                queryMethodMetaDataMap.put(method, metaData);
            }
        });
    }

    private QueryMethodMetaData createMetaData(Method method) {
        log.debug("extracting method: {}", method.getName());
        Class<?> returnTypeClass = method.getReturnType();
        if(!isReactorType(returnTypeClass)){
            throw new RuntimeException(toString(method) + " return type must be either Mono<T>, Flux<T>, or Mono<Page<T>> if pageable is present");
        }
        QueryMethodMetaData metaData = new QueryMethodMetaData();
        metaData.params = extractParamNames(method);

        Type returnType = method.getGenericReturnType();

        metaData.isNoResult = isNestTypePresent(returnType, Mono.class, Void.class);
        metaData.isSingleResult = isNestTypePresent(returnType, Mono.class, entityMetaData.entityClass);

        boolean isMonoList = isNestTypePresent(returnType, Mono.class, Page.class, entityMetaData.entityClass);
        log.debug("isMonoList: {}", isMonoList);
        boolean isFlux = isNestTypePresent(returnType,Flux.class, entityMetaData.entityClass);
        metaData.isReturningFlux = isFlux;
        log.debug("isFlux: {}", isFlux);

        metaData.isListResult = isMonoList || isFlux;

        //boolean paged = isNestTypePresent(returnType, Mono.class, Page.class, entityMetaData.entityClass);
        boolean isPageablePresent = isPageArgPresent(method);
        if(isMonoList && !isPageablePresent) {
            throw new RuntimeException(toString(method) + " the last parameter must be Pageable");
        }

        return metaData;
    }

    private boolean isNestTypePresent(Type returnType, Class<?>... clazz ) {
        return isNestTypePresent(returnType,  0, clazz);
    }
    private boolean isNestTypePresent(Type type, int idx, Class<?>... clazz) {
        if(idx >= clazz.length) return true;

        Class<?> clzz;
        if(type instanceof ParameterizedType) {
            clzz = (Class<?>)((ParameterizedType) type).getRawType();
            if(!isClzz(clzz, clazz[idx])){
                return false;
            }
            log.debug("raw-type-0: {}", clzz);
            for(Type actualType : ((ParameterizedType) type).getActualTypeArguments()) {
                boolean match = isNestTypePresent(actualType, idx + 1, clzz);
                if(match) {
                    return true;
                }
            }
            //return false;
        }
        clzz = (Class<?>)type;
        boolean ret = idx == clazz.length - 1 && isClzz(clzz, clazz[idx]);
        log.debug("raw-type-1: {} {}", clzz, ret);
        return ret;
    }
    private String[] extractParamNames(Method method) {
        Parameter[] params = method.getParameters();
        List<String> ret = new ArrayList<>();
        //String[] ret = new String[params.length];
        for (int i = 0; i < params.length; i++) {
            if(isNonSqlParamType(params[i])){
                continue;
            }
            Param param = params[i].getAnnotation(Param.class);
            String nm = params[i].getName();
            if(param != null) {
                nm = param.value();
            }
            ret.add(nm);
        }
        return ret.toArray(new String[]{});
    }
    private boolean isNonSqlParamType(Parameter p) {
        Class<?> type = p.getType();
        return isClzz(type, Pageable.class);
    }
    private boolean isReactorType(Class<?> clzz) {
        return isClzz(clzz, Mono.class) || isClzz(clzz, Flux.class);
    }
    private boolean isClzz(Class<?> clzz, Class<?>cls) {
        return clzz == cls || clzz.isAssignableFrom(cls);
    }
    private boolean isPageArgPresent(Method method) {
        Class<?> [] params = method.getParameterTypes();
        if(params.length == 0) return false;
        Class<?> last = params[params.length - 1];
        return last == Pageable.class || last.isAssignableFrom(Pageable.class);
    }
    private String toString(Method method) {
        return method.toString();
    }
    private boolean isQueryMethod(Method method) {
        Query query = method.getAnnotation(Query.class);
        return query != null;
    }
    public final EntityMetaData getEntityMetaData() {
        return entityMetaData;
    }

    private static String getIdColumnName(Class<?> entityClass) {
        log.debug("entityClass: {}", entityClass);
        Field idField = getIdColField(entityClass);
        return getIdColumnName(idField);
    }
    private static Field getIdColField(Class<?> entityClass) {
        Field idField = findByAnnotation(entityClass.getFields(), Id.class);
        if(idField == null) {
            idField = findByAnnotation(entityClass.getDeclaredFields(), Id.class);
        }
        return idField;
    }
    private static String getIdColumnName(Field idField) {
        Column idCol = idField.getAnnotation(Column.class);
        String colName = null;
        if(idCol == null){
            colName = idField.getName();
        }else{
            colName = idCol.name();
        }
        return colName;
    }
    private static Field findByAnnotation(Field[] fields, Class<? extends Annotation> annotationClass) {
        for(Field f : fields) {
            Annotation anno = f.getAnnotation(annotationClass);
            if(anno != null)
                return f;
            anno = f.getDeclaredAnnotation(annotationClass);
            if(anno != null)
                return f;
            if(f.getAnnotatedType().isAnnotationPresent(annotationClass)){
                return f;
            }
        }
        return null;
    }

    private Class<?> findEntityClass() {
        return (Class<?>)find(this.repoInterfaceClass).getActualTypeArguments()[0];//.getClass();
    }
    private Class<?> findIdClass() {
        return (Class<?>)find(this.repoInterfaceClass).getActualTypeArguments()[1];//.getClass();
    }

    private static ParameterizedType find(Class<?> repoInterface ) {
        ParameterizedType parameterizedTypes = extractParameterizedTypes(repoInterface);
        log.debug("method-param-types: {}", parameterizedTypes);
        return parameterizedTypes;
    }

    private static ParameterizedType extractParameterizedTypes(Class<?> repoInterface) {

        Type[] parameterizedTypes = repoInterface.getGenericInterfaces();
        for (Type parameterizedType : parameterizedTypes) {
            if(parameterizedType instanceof ParameterizedType) {
                return ((ParameterizedType)parameterizedType);
            }
        }
        return null;
    }

    public QueryMethodMetaData getQueryMethodMetaData(Method method) {
        return queryMethodMetaDataMap.get(method);
    }

    @Getter
    public static class QueryMethodMetaData {
        private String[] params;
        private boolean isSingleResult;
        private boolean isNoResult;
        private boolean isListResult;
        private boolean isReturningFlux;

        public boolean isResultList() {
            return isListResult;
        }


    }

}
