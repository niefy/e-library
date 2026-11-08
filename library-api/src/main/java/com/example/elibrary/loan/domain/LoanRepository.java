package com.example.elibrary.loan.domain;

import com.example.elibrary.common.paging.PageQuery;
import com.example.elibrary.common.paging.PageResult;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface LoanRepository {

    Optional<Loan> findById(UUID id);

    /**
     * Locks the loan row when supported. Return operations use this to make concurrent returns
     * converge on the already-returned state instead of racing on the version column.
     */
    Optional<Loan> findByIdForUpdate(UUID id);

    Optional<Loan> findActiveByUserAndBook(String userId, UUID bookId);

    long countActiveByBook(UUID bookId);

    Map<UUID, Long> countActiveByBookIds(Collection<UUID> bookIds);

    Loan save(Loan loan);

    PageResult<Loan> findByUser(String userId, LoanStatus status, PageQuery page);
}
