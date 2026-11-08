package com.example.elibrary.loan.application;

import com.example.elibrary.catalog.domain.Book;
import com.example.elibrary.catalog.domain.BookNotFoundException;
import com.example.elibrary.catalog.domain.BookRepository;
import com.example.elibrary.identity.Actor;
import com.example.elibrary.loan.domain.BookNotAvailableException;
import com.example.elibrary.loan.domain.Loan;
import com.example.elibrary.loan.domain.LoanAccessDeniedException;
import com.example.elibrary.loan.domain.LoanAlreadyActiveException;
import com.example.elibrary.loan.domain.LoanNotFoundException;
import com.example.elibrary.loan.domain.LoanRepository;
import java.time.Clock;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoanService {

    private static final Logger log = LoggerFactory.getLogger(LoanService.class);

    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;
    private final Clock clock;

    public LoanService(
            BookRepository bookRepository, LoanRepository loanRepository, Clock clock) {
        this.bookRepository = bookRepository;
        this.loanRepository = loanRepository;
        this.clock = clock;
    }

    /**
     * Borrowing locks the Book row first. All borrow requests for one book therefore serialize on
     * that row, and the active-loan count read afterwards is stable for the rest of the
     * transaction. A partial unique index additionally guarantees one active loan per user/book
     * even if the lock is unavailable (SQLite) or bypassed.
     */
    @Transactional
    public LoanView borrow(UUID bookId, Actor actor) {
        Book book = bookRepository
                .findByIdForUpdate(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));

        // Check the caller's own active loan first so a repeated request reports the accurate
        // conflict even when the last copy has just been taken by that same request.
        loanRepository
                .findActiveByUserAndBook(actor.userId(), bookId)
                .ifPresent(existing -> {
                    throw new LoanAlreadyActiveException(bookId);
                });
        long activeLoans = loanRepository.countActiveByBook(bookId);
        if (activeLoans >= book.getTotalCopies()) {
            throw new BookNotAvailableException(bookId);
        }

        Loan loan = Loan.borrow(bookId, actor.userId(), clock.instant());
        loanRepository.save(loan);
        log.info(
                "Borrowed loanId={} bookId={} userId={} dueAt={}",
                loan.getId(),
                bookId,
                actor.userId(),
                loan.getDueAt());
        return LoanView.of(loan, book.getTitle());
    }

    /**
     * Returns are idempotent. The loan row is locked when the database supports it; in all cases
     * the status transition is guarded by the aggregate and by optimistic versioning.
     */
    @Transactional
    public LoanView returnLoan(UUID loanId, Actor actor) {
        Loan loan = loanRepository
                .findByIdForUpdate(loanId)
                .orElseThrow(() -> new LoanNotFoundException(loanId));
        if (!actor.canAccess(loan.getUserId())) {
            throw new LoanAccessDeniedException();
        }

        boolean changed = loan.returnBook(clock.instant());
        if (!changed) {
            log.debug("Loan {} was already returned; returning idempotent response", loanId);
        }

        Book book = bookRepository
                .findById(loan.getBookId())
                .orElseThrow(() -> new BookNotFoundException(loan.getBookId()));
        return LoanView.of(loan, book.getTitle());
    }
}
