package com.example.elibrary.loan.application;

import com.example.elibrary.catalog.domain.Book;
import com.example.elibrary.catalog.domain.BookRepository;
import com.example.elibrary.identity.Actor;
import com.example.elibrary.loan.domain.Loan;
import com.example.elibrary.loan.domain.LoanAccessDeniedException;
import com.example.elibrary.loan.domain.LoanNotFoundException;
import com.example.elibrary.loan.domain.LoanRepository;
import com.example.elibrary.loan.domain.LoanStatus;
import com.example.elibrary.shared.paging.PageQuery;
import com.example.elibrary.shared.paging.PageResult;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoanQueryService {

    private static final String UNKNOWN_BOOK_TITLE = "Unknown book";

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;

    public LoanQueryService(LoanRepository loanRepository, BookRepository bookRepository) {
        this.loanRepository = loanRepository;
        this.bookRepository = bookRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<LoanView> myLoans(Actor actor, LoanStatus status, PageQuery page) {
        PageResult<Loan> result = loanRepository.findByUser(actor.userId(), status, page);
        Map<UUID, Book> booksById = loadBooks(result.items());
        return result.map(loan -> toView(loan, booksById));
    }

    @Transactional(readOnly = true)
    public LoanView getLoan(UUID loanId, Actor actor) {
        Loan loan = loanRepository
                .findById(loanId)
                .orElseThrow(() -> new LoanNotFoundException(loanId));
        if (!actor.canAccess(loan.getUserId())) {
            throw new LoanAccessDeniedException();
        }
        Map<UUID, Book> booksById = loadBooks(List.of(loan));
        return toView(loan, booksById);
    }

    private Map<UUID, Book> loadBooks(List<Loan> loans) {
        List<UUID> bookIds =
                loans.stream().map(Loan::getBookId).distinct().toList();
        return bookRepository.findAllByIds(bookIds).stream()
                .collect(Collectors.toMap(Book::getId, Function.identity()));
    }

    private static LoanView toView(Loan loan, Map<UUID, Book> booksById) {
        Book book = booksById.get(loan.getBookId());
        String title = book == null ? UNKNOWN_BOOK_TITLE : book.getTitle();
        return LoanView.of(loan, title);
    }
}
