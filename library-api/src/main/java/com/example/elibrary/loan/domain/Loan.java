package com.example.elibrary.loan.domain;

import com.example.elibrary.shared.persistence.InstantEpochMillisConverter;
import com.example.elibrary.shared.persistence.UuidVarcharConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/**
 * Loan aggregate root.
 *
 * <p>State changes go through {@link #borrow} and {@link #returnBook}; there are no setters. The
 * return operation is idempotent: returning an already returned loan is a no-op. The aggregate
 * references other aggregates only by id.
 */
@Entity
@Table(name = "loan")
public class Loan {

    public static final Duration LOAN_PERIOD = Duration.ofDays(14);
    private static final int MAX_USER_ID_LENGTH = 100;

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private UUID id;

    @Convert(converter = UuidVarcharConverter.class)
    @Column(name = "book_id", nullable = false, updatable = false, length = 36)
    private UUID bookId;

    @Column(name = "user_id", nullable = false, updatable = false, length = MAX_USER_ID_LENGTH)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LoanStatus status;

    @Convert(converter = InstantEpochMillisConverter.class)
    @Column(name = "borrowed_at", nullable = false, updatable = false)
    private Instant borrowedAt;

    @Convert(converter = InstantEpochMillisConverter.class)
    @Column(name = "due_at", nullable = false, updatable = false)
    private Instant dueAt;

    @Convert(converter = InstantEpochMillisConverter.class)
    @Column(name = "returned_at")
    private Instant returnedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    /** Required by JPA. Application code must use {@link #borrow}. */
    protected Loan() {
    }

    private Loan(
            UUID id,
            UUID bookId,
            String userId,
            LoanStatus status,
            Instant borrowedAt,
            Instant dueAt,
            Instant returnedAt) {
        this.id = id;
        this.bookId = bookId;
        this.userId = userId;
        this.status = status;
        this.borrowedAt = borrowedAt;
        this.dueAt = dueAt;
        this.returnedAt = returnedAt;
    }

    public static Loan borrow(UUID bookId, String userId, Instant now) {
        Objects.requireNonNull(bookId, "bookId");
        Objects.requireNonNull(now, "now");
        String normalizedUserId = normalizeUserId(userId);
        Instant borrowedAt = now.truncatedTo(ChronoUnit.MILLIS);

        return new Loan(
                UUID.randomUUID(),
                bookId,
                normalizedUserId,
                LoanStatus.ACTIVE,
                borrowedAt,
                borrowedAt.plus(LOAN_PERIOD),
                null);
    }

    /**
     * @return true when this call changed the state, false when the loan was already returned.
     */
    public boolean returnBook(Instant now) {
        Objects.requireNonNull(now, "now");
        if (status == LoanStatus.RETURNED) {
            return false;
        }
        this.status = LoanStatus.RETURNED;
        this.returnedAt = now.truncatedTo(ChronoUnit.MILLIS);
        return true;
    }

    public boolean isActive() {
        return status == LoanStatus.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public UUID getBookId() {
        return bookId;
    }

    public String getUserId() {
        return userId;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public Instant getBorrowedAt() {
        return borrowedAt;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public Instant getReturnedAt() {
        return returnedAt;
    }

    public long getVersion() {
        return version;
    }

    private static String normalizeUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        String trimmed = userId.trim();
        if (trimmed.length() > MAX_USER_ID_LENGTH) {
            throw new IllegalArgumentException(
                    "userId must be at most " + MAX_USER_ID_LENGTH + " characters");
        }
        return trimmed;
    }
}
