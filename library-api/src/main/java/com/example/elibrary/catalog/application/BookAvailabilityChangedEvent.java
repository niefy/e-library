package com.example.elibrary.catalog.application;

import java.util.Objects;
import java.util.UUID;

/**
 * Signals that the number of active loans for a book changed. The event is published inside the
 * borrow/return transaction and only handled after a successful commit.
 */
public record BookAvailabilityChangedEvent(UUID bookId) {

    public BookAvailabilityChangedEvent {
        Objects.requireNonNull(bookId, "bookId");
    }
}
