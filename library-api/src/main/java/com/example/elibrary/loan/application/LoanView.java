package com.example.elibrary.loan.application;

import com.example.elibrary.loan.domain.Loan;
import com.example.elibrary.loan.domain.LoanStatus;
import java.time.Instant;
import java.util.UUID;

public record LoanView(
        UUID id,
        UUID bookId,
        String bookTitle,
        LoanStatus status,
        Instant borrowedAt,
        Instant dueAt,
        Instant returnedAt) {

    public static LoanView of(Loan loan, String bookTitle) {
        return new LoanView(
                loan.getId(),
                loan.getBookId(),
                bookTitle,
                loan.getStatus(),
                loan.getBorrowedAt(),
                loan.getDueAt(),
                loan.getReturnedAt());
    }
}
