package com.example.elibrary.catalog.domain;

import com.example.elibrary.shared.error.DomainException;
import com.example.elibrary.shared.error.ErrorCode;
import java.util.UUID;

public class BookNotFoundException extends DomainException {

    public BookNotFoundException(UUID bookId) {
        super(ErrorCode.BOOK_NOT_FOUND, "Book " + bookId + " was not found");
    }
}
