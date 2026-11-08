package com.example.elibrary.loan.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LoanTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:15:30.123456Z");

    @Test
    void createsActiveLoanWithFourteenDayDueDate() {
        UUID bookId = UUID.randomUUID();

        Loan loan = Loan.borrow(bookId, "alice", NOW);

        assertThat(loan.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(loan.getBookId()).isEqualTo(bookId);
        assertThat(loan.getUserId()).isEqualTo("alice");
        assertThat(loan.getBorrowedAt()).isEqualTo(Instant.parse("2026-01-15T10:15:30.123Z"));
        assertThat(loan.getDueAt()).isEqualTo(loan.getBorrowedAt().plus(Loan.LOAN_PERIOD));
        assertThat(loan.getReturnedAt()).isNull();
        assertThat(loan.isActive()).isTrue();
    }

    @Test
    void returningActiveLoanMovesItToReturnedState() {
        Loan loan = Loan.borrow(UUID.randomUUID(), "alice", NOW);
        Instant returnedAt = NOW.plusSeconds(60);

        boolean changed = loan.returnBook(returnedAt);

        assertThat(changed).isTrue();
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.RETURNED);
        assertThat(loan.getReturnedAt()).isEqualTo(returnedAt.truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
        assertThat(loan.isActive()).isFalse();
    }

    @Test
    void returningAgainIsIdempotentAndPreservesOriginalTimestamp() {
        Loan loan = Loan.borrow(UUID.randomUUID(), "alice", NOW);
        Instant firstReturn = NOW.plusSeconds(60);
        loan.returnBook(firstReturn);
        Instant firstReturnedAt = loan.getReturnedAt();

        boolean changed = loan.returnBook(firstReturn.plusSeconds(3600));

        assertThat(changed).isFalse();
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.RETURNED);
        assertThat(loan.getReturnedAt()).isEqualTo(firstReturnedAt);
    }

    @Test
    void rejectsMissingBookOrBlankUser() {
        assertThatThrownBy(() -> Loan.borrow(null, "alice", NOW))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("bookId");

        assertThatThrownBy(() -> Loan.borrow(UUID.randomUUID(), " ", NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId");
    }

    @Test
    void truncatesTimestampsToDatabasePrecision() {
        Loan loan = Loan.borrow(UUID.randomUUID(), "alice", NOW);

        assertThat(loan.getBorrowedAt().getNano() % 1_000_000).isZero();
        assertThat(loan.getDueAt().getNano() % 1_000_000).isZero();
    }
}
