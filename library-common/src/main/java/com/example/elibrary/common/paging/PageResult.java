package com.example.elibrary.common.paging;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Framework-independent page result used by domain repositories and application services.
 */
public record PageResult<T>(List<T> items, int page, int size, long totalElements) {

    public PageResult {
        Objects.requireNonNull(items, "items");
        items = List.copyOf(items);
    }

    public int totalPages() {
        if (size == 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalElements / size);
    }

    public <R> PageResult<R> map(Function<? super T, ? extends R> mapper) {
        List<R> mapped = new ArrayList<>(items.size());
        for (T item : items) {
            mapped.add(mapper.apply(item));
        }
        return new PageResult<>(mapped, page, size, totalElements);
    }
}
