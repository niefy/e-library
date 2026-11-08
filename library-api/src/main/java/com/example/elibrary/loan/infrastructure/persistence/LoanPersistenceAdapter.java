package com.example.elibrary.loan.infrastructure.persistence;

import com.example.elibrary.loan.domain.BookNotAvailableException;
import com.example.elibrary.loan.domain.Loan;
import com.example.elibrary.loan.domain.LoanAlreadyActiveException;
import com.example.elibrary.loan.domain.LoanRepository;
import com.example.elibrary.loan.domain.LoanStatus;
import com.example.elibrary.shared.paging.PageQuery;
import com.example.elibrary.shared.paging.PageResult;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class LoanPersistenceAdapter implements LoanRepository {

    private static final String ACTIVE_UNIQUE_INDEX = "uq_loan_active_user_book";
    private static final String CAPACITY_TRIGGER_MARKER = "loan_capacity_exceeded";

    private final LoanSpringDataRepository repository;

    public LoanPersistenceAdapter(LoanSpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Loan> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public Optional<Loan> findByIdForUpdate(UUID id) {
        return repository.findByIdForUpdate(id);
    }

    @Override
    public Optional<Loan> findActiveByUserAndBook(String userId, UUID bookId) {
        return repository.findByUserIdAndBookIdAndStatus(userId, bookId, LoanStatus.ACTIVE);
    }

    @Override
    public long countActiveByBook(UUID bookId) {
        return repository.countByBookIdAndStatus(bookId, LoanStatus.ACTIVE);
    }

    @Override
    public Map<UUID, Long> countActiveByBookIds(Collection<UUID> bookIds) {
        if (bookIds == null || bookIds.isEmpty()) {
            return Map.of();
        }
        return repository.countByBookIdsAndStatus(bookIds, LoanStatus.ACTIVE).stream()
                .collect(Collectors.toMap(
                        LoanSpringDataRepository.ActiveLoanCount::getBookId,
                        LoanSpringDataRepository.ActiveLoanCount::getCount));
    }

    /**
     * Flushing here makes database constraints fail inside the transaction, where they can be
     * translated into domain conflicts. The constraints remain the final authority.
     */
    @Override
    public Loan save(Loan loan) {
        try {
            return repository.saveAndFlush(loan);
        } catch (DataIntegrityViolationException exception) {
            throw translate(exception, loan);
        }
    }

    @Override
    public PageResult<Loan> findByUser(String userId, LoanStatus status, PageQuery page) {
        Pageable pageable = PageRequest.of(
                page.page(),
                page.size(),
                Sort.by(Sort.Order.desc("borrowedAt"), Sort.Order.asc("id")));
        Page<Loan> result = status == null
                ? repository.findByUserId(userId, pageable)
                : repository.findByUserIdAndStatus(userId, status, pageable);
        return new PageResult<>(
                result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements());
    }

    private static RuntimeException translate(DataIntegrityViolationException exception, Loan loan) {
        String message = deepestMessage(exception).toLowerCase(Locale.ROOT);
        if (message.contains(CAPACITY_TRIGGER_MARKER)) {
            return new BookNotAvailableException(loan.getBookId());
        }
        if (message.contains(ACTIVE_UNIQUE_INDEX)
                || (message.contains("unique") && message.contains("user_id"))) {
            return new LoanAlreadyActiveException(loan.getBookId());
        }
        return exception;
    }

    private static String deepestMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current.getMessage() == null ? "" : current.getMessage();
    }
}
