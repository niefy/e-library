package com.example.elibrary.loan.domain;

import com.example.elibrary.shared.error.DomainException;
import com.example.elibrary.shared.error.ErrorCode;
import java.util.UUID;

public class BookNotAvailableException extends DomainException {

    public BookNotAvailableException(UUID bookId) {
        super(ErrorCode.BOOK_NOT_AVAILABLE, "Book " + bookId + " has no available copies");
    }
}
