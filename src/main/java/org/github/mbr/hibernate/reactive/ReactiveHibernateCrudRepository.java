package org.github.mbr.hibernate.reactive;

import io.smallrye.mutiny.Uni;
import org.github.mbr.hibernate.reactive.impl.annotations.RepositoryMethod;
import org.hibernate.reactive.mutiny.Mutiny;
import org.hibernate.reactive.stage.Stage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ReactiveHibernateCrudRepository<E, ID> {
    @RepositoryMethod
    //Uni<List<E>> findAll();
    Flux<E> findAll();

    @RepositoryMethod
    //Uni<List<E>> findAllById(Iterable<? extends ID> ids);
    Flux<E> findAllById(Iterable<? extends ID> ids);

    @RepositoryMethod
    Mono<E> findById(ID id);

    @RepositoryMethod
    //Uni<E> save(E e);
    Mono<E> save(E e);

    @RepositoryMethod
    //Uni<List<E>> saveAll(Iterable<E> entities);
    Flux<E> saveAll(Iterable<E> entities);

    @RepositoryMethod
    //Uni<Integer> deleteById(ID id);
    Mono<Integer> deleteById(ID id);

    @RepositoryMethod
    //Uni<Integer> deleteAllById(Iterable<? extends ID> ids);
    Mono<Integer> deleteAllById(Iterable<? extends ID> ids);

    @RepositoryMethod
    //Uni<Integer> deleteAll();
    Mono<Integer> deleteAll();

    @RepositoryMethod
    Mutiny.SessionFactory getMutinySessionFactory();

    @RepositoryMethod
    Stage.SessionFactory getStageSessionFactory();
}
