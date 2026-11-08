package com.example.elibrary.common.error;

/**
 * Base class for failures that are part of the domain contract. Domain code stays free of HTTP
 * types; the web adapter translates these exceptions to RFC 7807 responses.
 */
public abstract class DomainException extends RuntimeException {

    private final ErrorCode errorCode;

    protected DomainException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
