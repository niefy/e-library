package com.example.elibrary.catalog.infrastructure.persistence;

import com.example.elibrary.catalog.domain.Book;
import com.example.elibrary.catalog.domain.BookRepository;
import com.example.elibrary.shared.paging.PageQuery;
import com.example.elibrary.shared.paging.PageResult;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class BookPersistenceAdapter implements BookRepository {

    private final BookSpringDataRepository repository;

    public BookPersistenceAdapter(BookSpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Book> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public Optional<Book> findByIdForUpdate(UUID id) {
        return repository.findByIdForUpdate(id);
    }

    @Override
    public PageResult<Book> search(String query, PageQuery page) {
        Pageable pageable = PageRequest.of(
                page.page(),
                page.size(),
                Sort.by(Sort.Order.asc("title"), Sort.Order.asc("id")));
        Page<Book> result = repository.search(
                query == null ? "" : query.toLowerCase(Locale.ROOT), pageable);
        return new PageResult<>(
                result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Override
    public List<Book> findAllByIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return repository.findAllById(new LinkedHashSet<>(ids));
    }

    @Override
    public Book save(Book book) {
        return repository.saveAndFlush(book);
    }
}
