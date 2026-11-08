package com.example.elibrary.catalog.domain;

import com.example.elibrary.common.error.DomainException;
import com.example.elibrary.common.error.ErrorCode;
import java.util.UUID;

public class BookNotFoundException extends DomainException {

    public BookNotFoundException(UUID bookId) {
        super(ErrorCode.BOOK_NOT_FOUND, "Book " + bookId + " was not found");
    }
}
