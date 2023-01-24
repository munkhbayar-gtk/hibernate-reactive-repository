package io.github.mbr.hibernate.reactive.data;

import lombok.AllArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Function;

@AllArgsConstructor
public class Page<T> {

    public final long totalElements;
    public final int totalPages;
    public final List<T> data;

    public static <T> Page<T> of(long totalElements, int totalPages, List<T> data) {
        return new Page<>(totalElements, totalPages, data);
    }

    //<U> Page<U> map(Function<? super T, ? extends U> converter);
}
