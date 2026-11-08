package com.example.elibrary.catalog.adapter.in.web;

import com.example.elibrary.catalog.application.BookView;
import java.time.Instant;
import java.util.UUID;

/**
 * Wire DTO. Domain entities never leave the application boundary.
 */
public record BookResponse(
        UUID id,
        String isbn,
        String title,
        String author,
        int totalCopies,
        int availableCopies,
        Instant createdAt) {

    public static BookResponse from(BookView view) {
        return new BookResponse(
                view.id(),
                view.isbn(),
                view.title(),
                view.author(),
                view.totalCopies(),
                view.availableCopies(),
                view.createdAt());
    }
}
