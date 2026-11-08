package com.example.elibrary.catalog.domain;

import com.example.elibrary.shared.persistence.InstantEpochMillisConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Catalog aggregate root. It owns bibliographic data only; availability is derived from active
 * loans by the application layer and protected by database constraints.
 *
 * <p>JPA annotations are deliberately minimal. There are no setters: a Book is created through a
 * factory and its fields are immutable.
 */
@Entity
@Table(name = "book")
public class Book {

    private static final Pattern ISBN_PATTERN = Pattern.compile("^(?:\\d{9}[\\dX]|\\d{13})$");
    private static final int MAX_TEXT_LENGTH = 255;

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private UUID id;

    @Column(name = "isbn", nullable = false, unique = true, length = 20)
    private String isbn;

    @Column(name = "title", nullable = false, length = MAX_TEXT_LENGTH)
    private String title;

    @Column(name = "author", nullable = false, length = MAX_TEXT_LENGTH)
    private String author;

    @Column(name = "total_copies", nullable = false)
    private int totalCopies;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Convert(converter = InstantEpochMillisConverter.class)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Required by JPA. Application code must use {@link #create}. */
    protected Book() {
    }

    private Book(
            UUID id,
            String isbn,
            String title,
            String author,
            int totalCopies,
            Instant createdAt) {
        this.id = id;
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.totalCopies = totalCopies;
        this.createdAt = createdAt;
    }

    public static Book create(
            String isbn, String title, String author, int totalCopies, Instant createdAt) {
        String normalizedIsbn = normalizeIsbn(isbn);
        return new Book(
                UUID.randomUUID(),
                normalizedIsbn,
                requireText(title, "title"),
                requireText(author, "author"),
                requirePositiveCopies(totalCopies),
                truncate(Objects.requireNonNull(createdAt, "createdAt")));
    }

    public UUID getId() {
        return id;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public long getVersion() {
        return version;
    }

    private static String normalizeIsbn(String rawIsbn) {
        String normalized = requireText(rawIsbn, "isbn").replace("-", "").replace(" ", "");
        normalized = normalized.toUpperCase(Locale.ROOT);
        if (!ISBN_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("isbn must be a valid ISBN-10 or ISBN-13 value");
        }
        return normalized;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException(field + " must be at most " + MAX_TEXT_LENGTH + " characters");
        }
        return trimmed;
    }

    private static int requirePositiveCopies(int totalCopies) {
        if (totalCopies < 1) {
            throw new IllegalArgumentException("totalCopies must be at least 1");
        }
        return totalCopies;
    }

    private static Instant truncate(Instant instant) {
        return instant.truncatedTo(ChronoUnit.MILLIS);
    }
}
