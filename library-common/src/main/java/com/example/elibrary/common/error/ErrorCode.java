package com.example.elibrary.common.error;

import java.util.Locale;

/**
 * Stable machine-readable error codes exposed by the API. The numeric HTTP status is kept here so
 * every adapter maps the same domain failure to the same transport semantics.
 */
public enum ErrorCode {
    BOOK_NOT_FOUND(404, "Book not found"),
    LOAN_NOT_FOUND(404, "Loan not found"),
    RESOURCE_NOT_FOUND(404, "Resource not found"),
    BOOK_NOT_AVAILABLE(409, "Book is not available"),
    LOAN_ALREADY_ACTIVE(409, "An active loan already exists"),
    CONCURRENT_MODIFICATION(409, "Concurrent modification"),
    DATA_CONFLICT(409, "Request conflicts with existing data"),
    ACCESS_DENIED(403, "Access denied"),
    UNAUTHORIZED(401, "Authentication required"),
    VALIDATION_FAILED(400, "Request validation failed"),
    MALFORMED_REQUEST(400, "Malformed request"),
    METHOD_NOT_ALLOWED(405, "Method not allowed"),
    INTERNAL_ERROR(500, "Internal server error");

    private final int httpStatus;
    private final String title;

    ErrorCode(int httpStatus, String title) {
        this.httpStatus = httpStatus;
        this.title = title;
    }

    public int httpStatus() {
        return httpStatus;
    }

    public String title() {
        return title;
    }

    public String slug() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
