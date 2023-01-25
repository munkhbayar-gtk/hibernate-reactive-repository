package io.github.mbr.hibernate.reactive;

import io.github.mbr.hibernate.reactive.impl.annotations.RepositoryMethod;
import io.github.mbr.hibernate.reactive.impl.annotations.RepositoryPagedMethod;
import org.hibernate.reactive.mutiny.Mutiny;
import org.hibernate.reactive.stage.Stage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReactiveHibernateCrudRepository<E, ID> {
    @RepositoryMethod
    //Uni<List<E>> findAll();
    Flux<E> findAll();

    @RepositoryPagedMethod
    Mono<Page<E>> findAll(Pageable pageable);

    @RepositoryMethod
    //Uni<List<E>> findAllById(Iterable<? extends ID> ids);

    Flux<E> findAllById(Iterable<? extends ID> ids);

    @RepositoryPagedMethod
    Mono<Page<E>> findAllById(Iterable<? extends ID> ids, Pageable pageable);

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
