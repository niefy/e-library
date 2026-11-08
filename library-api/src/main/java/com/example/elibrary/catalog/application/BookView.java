package com.example.elibrary.catalog.application;

import com.example.elibrary.catalog.domain.Book;
import java.time.Instant;
import java.util.UUID;

public record BookView(
        UUID id,
        String isbn,
        String title,
        String author,
        int totalCopies,
        int availableCopies,
        Instant createdAt) {

    public static BookView of(Book book, long activeLoans) {
        long available = Math.max(0L, (long) book.getTotalCopies() - activeLoans);
        return new BookView(
                book.getId(),
                book.getIsbn(),
                book.getTitle(),
                book.getAuthor(),
                book.getTotalCopies(),
                (int) Math.min(Integer.MAX_VALUE, available),
                book.getCreatedAt());
    }
}
