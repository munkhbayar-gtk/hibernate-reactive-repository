package io.github.mbr.hibernate.reactive.config;

import io.smallrye.mutiny.Uni;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UniToReactorConvertors {
    static Mono<?> toMono(Uni<?> uni) {
        return null;
    }
    static Flux<?> toFlux(Uni<?> uni) {
        return null;
    }
}
