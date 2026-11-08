package com.example.elibrary.loan.adapter.in.web;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Note the absence of a userId field. The caller identity is taken only from the bearer token.
 */
public record BorrowBookRequest(@NotNull(message = "must not be null") UUID bookId) {
}
