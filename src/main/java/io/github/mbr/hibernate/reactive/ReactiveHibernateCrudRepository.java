package io.github.mbr.hibernate.reactive;

import io.github.mbr.hibernate.reactive.data.GetAssociation;
import io.github.mbr.hibernate.reactive.data.SetAssociation;
import io.github.mbr.hibernate.reactive.impl.annotations.RepositoryMethod;
import io.github.mbr.hibernate.reactive.impl.annotations.RepositoryPagedMethod;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.uni.UniReactorConverters;
import org.hibernate.reactive.mutiny.Mutiny;
import org.hibernate.reactive.stage.Stage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static io.smallrye.mutiny.converters.uni.UniReactorConverters.toMono;

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

    default <T> Mono<T> loadAssociations(GetAssociation<T> getter, SetAssociation<T> setter) {
        var sf = getMutinySessionFactory();
        var uni = sf.withStatelessSession(session->
            session.fetch(getter.get())
        );
        return to_Mono(uni).flatMap(links->{
            setter.set(links);
            return Mono.justOrEmpty(links);
        });
    }

    default <T>Mono<T> to_Mono(Uni<T> uni) {
        return uni.convert().with(toMono());
    }
    default <T>Flux<T> to_Flux(Uni<T> uni) {
        Mono<?> mono = uni.convert().with(UniReactorConverters.toMono());
        return mono.flatMapMany((item)->{
            List<T> list = (List<T>) item;
            return Flux.fromIterable(list);
        });
    }
}
