package org.github.mbr.hibernate.reactive;

import io.smallrye.mutiny.Uni;
import org.github.mbr.hibernate.reactive.impl.annotations.RepositoryMethod;
import org.hibernate.reactive.mutiny.Mutiny;
import org.hibernate.reactive.stage.Stage;

import java.util.List;

public interface ReactiveHibernateCrudRepository<E, ID> {
    @RepositoryMethod
    wzUni<List<E>> findAll();

    @RepositoryMethod
    Uni<List<E>> findAllById(Iterable<? extends ID> ids);

    @RepositoryMethod
    Uni<E> findById(ID id);

    @RepositoryMethod
    Uni<E> save(E e);

    @RepositoryMethod
    Uni<List<E>> saveAll(Iterable<E> entities);

    @RepositoryMethod
    Uni<Integer> deleteById(ID id);

    @RepositoryMethod
    Uni<Integer> deleteAllById(Iterable<? extends ID> ids);

    @RepositoryMethod
    Uni<Integer> deleteAll();

    @RepositoryMethod
    Mutiny.SessionFactory getMutinySessionFactory();

    @RepositoryMethod
    Stage.SessionFactory getStageSessionFactory();
}
