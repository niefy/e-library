package com.example.elibrary.catalog.application;

import com.example.elibrary.catalog.domain.Book;
import com.example.elibrary.catalog.domain.BookNotFoundException;
import com.example.elibrary.catalog.domain.BookRepository;
import com.example.elibrary.loan.domain.LoanRepository;
import com.example.elibrary.shared.paging.PageQuery;
import com.example.elibrary.shared.paging.PageResult;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookQueryService {

    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;

    public BookQueryService(BookRepository bookRepository, LoanRepository loanRepository) {
        this.bookRepository = bookRepository;
        this.loanRepository = loanRepository;
    }

    @Transactional(readOnly = true)
    public BookView getBook(UUID bookId) {
        Book book = bookRepository
                .findById(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));
        long activeLoans = loanRepository.countActiveByBook(bookId);
        return BookView.of(book, activeLoans);
    }

    @Transactional(readOnly = true)
    public PageResult<BookView> searchBooks(String query, PageQuery page) {
        String normalizedQuery = query == null ? "" : query.trim();
        PageResult<Book> books = bookRepository.search(normalizedQuery, page);
        List<UUID> bookIds = books.items().stream().map(Book::getId).toList();
        Map<UUID, Long> activeLoans = loanRepository.countActiveByBookIds(bookIds);
        return books.map(book -> BookView.of(book, activeLoans.getOrDefault(book.getId(), 0L)));
    }
}
