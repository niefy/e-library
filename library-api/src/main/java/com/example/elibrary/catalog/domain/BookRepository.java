package com.example.elibrary.catalog.domain;

import com.example.elibrary.common.paging.PageQuery;
import com.example.elibrary.common.paging.PageResult;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain port. The JPA implementation lives in infrastructure; application services only see this
 * interface.
 */
public interface BookRepository {

    Optional<Book> findById(UUID id);

    /**
     * Acquires a row-level write lock on the book when the database supports it. Borrowing uses
     * this as the serialization point for the copies invariant.
     */
    Optional<Book> findByIdForUpdate(UUID id);

    PageResult<Book> search(String query, PageQuery page);

    List<Book> findAllByIds(Collection<UUID> ids);

    Book save(Book book);
}
