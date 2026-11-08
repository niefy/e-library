package com.example.elibrary.loan.infrastructure.persistence;

import com.example.elibrary.loan.domain.Loan;
import com.example.elibrary.loan.domain.LoanStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoanSpringDataRepository extends JpaRepository<Loan, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Loan l where l.id = :id")
    Optional<Loan> findByIdForUpdate(@Param("id") UUID id);

    Optional<Loan> findByUserIdAndBookIdAndStatus(
            String userId, UUID bookId, LoanStatus status);

    long countByBookIdAndStatus(UUID bookId, LoanStatus status);

    @Query("""
            select l.bookId as bookId, count(l) as count
            from Loan l
            where l.bookId in :bookIds and l.status = :status
            group by l.bookId
            """)
    List<ActiveLoanCount> countByBookIdsAndStatus(
            @Param("bookIds") Collection<UUID> bookIds, @Param("status") LoanStatus status);

    Page<Loan> findByUserId(String userId, Pageable pageable);

    Page<Loan> findByUserIdAndStatus(String userId, LoanStatus status, Pageable pageable);

    interface ActiveLoanCount {
        UUID getBookId();

        long getCount();
    }
}
