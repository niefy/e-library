package com.example.elibrary.common.paging;

import com.example.elibrary.common.paging.PageResult;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Explicit page envelope. Returning Spring's Page type would leak framework details and an
 * unstable JSON shape.
 */
public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static <T> PageResponse<T> from(PageResult<T> result) {
        return new PageResponse<>(
                result.items(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages());
    }

    public static <S, T> PageResponse<T> from(
            PageResult<S> result, Function<? super S, ? extends T> mapper) {
        List<T> mapped = new ArrayList<>(result.items().size());
        for (S item : result.items()) {
            mapped.add(mapper.apply(item));
        }
        return new PageResponse<>(
                mapped,
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages());
    }
}
