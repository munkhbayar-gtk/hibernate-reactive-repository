package io.github.mbr.hibernate.reactive.data;

@FunctionalInterface
public interface SetAssociation<T> {
    void set(T links);
}
