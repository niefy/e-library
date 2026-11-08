package com.example.elibrary.loan.adapter.in.web;

import com.example.elibrary.loan.application.LoanView;
import com.example.elibrary.loan.domain.LoanStatus;
import java.time.Instant;
import java.util.UUID;

public record LoanResponse(
        UUID id,
        UUID bookId,
        String bookTitle,
        LoanStatus status,
        Instant borrowedAt,
        Instant dueAt,
        Instant returnedAt) {

    public static LoanResponse from(LoanView view) {
        return new LoanResponse(
                view.id(),
                view.bookId(),
                view.bookTitle(),
                view.status(),
                view.borrowedAt(),
                view.dueAt(),
                view.returnedAt());
    }
}
