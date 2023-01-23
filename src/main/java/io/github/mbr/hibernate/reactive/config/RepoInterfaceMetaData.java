package io.github.mbr.hibernate.reactive.config;

import io.github.mbr.hibernate.reactive.ReactiveHibernateCrudRepository;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.Column;
import javax.persistence.Id;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

@Slf4j

public class RepoInterfaceMetaData {
    public final Class<? extends ReactiveHibernateCrudRepository<?,?>> repoInterfaceClass;
    private EntityMetaData entityMetaData;
    public static RepoInterfaceMetaData of(Class<? extends ReactiveHibernateCrudRepository<?,?>> repoInterfaceClass) {
        return new RepoInterfaceMetaData(repoInterfaceClass);
    }
    private RepoInterfaceMetaData(Class<? extends ReactiveHibernateCrudRepository<?,?>> repoInterfaceClass) {
        this.repoInterfaceClass = repoInterfaceClass;
        buildEntityMetaData();
    }

    private void buildEntityMetaData() {
        Class<?> entityClass = findEntityClass();
        Class<?> idClass = findIdClass();
        String idColName = getIdColumnName(entityClass);
        this.entityMetaData = new EntityMetaData(entityClass, idClass, idColName);
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
}
