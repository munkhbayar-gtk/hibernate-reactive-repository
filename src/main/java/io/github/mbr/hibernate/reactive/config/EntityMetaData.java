package io.github.mbr.hibernate.reactive.config;

public class EntityMetaData {

    public final String idColumnName;
    public final Class<?> entityClass;
    public final Class<?> idClass;

    public EntityMetaData(Class<?> entityClass, Class<?> idClass, String idColumnName) {
        this.idColumnName = idColumnName;
        this.entityClass = entityClass;
        this.idClass = idClass;
    }
}
