package com.example.elibrary.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BookTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:15:30.987654Z");

    @Test
    void createsBookAndNormalizesIsbn() {
        Book book = Book.create("978-0-13-235088-4", " Clean Code ", " Robert C. Martin ", 3, NOW);

        assertThat(book.getId()).isNotNull();
        assertThat(book.getIsbn()).isEqualTo("9780132350884");
        assertThat(book.getTitle()).isEqualTo("Clean Code");
        assertThat(book.getAuthor()).isEqualTo("Robert C. Martin");
        assertThat(book.getTotalCopies()).isEqualTo(3);
        assertThat(book.getCreatedAt()).isEqualTo(Instant.parse("2026-01-15T10:15:30.987Z"));
    }

    @Test
    void rejectsInvalidBibliographicData() {
        assertThatThrownBy(() -> Book.create("123", "Title", "Author", 1, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("isbn");
        assertThatThrownBy(() -> Book.create("9780132350884", " ", "Author", 1, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title");
        assertThatThrownBy(() -> Book.create("9780132350884", "Title", "Author", 0, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("totalCopies");
    }

    @Test
    void acceptsIsbn10WithXCheckDigit() {
        Book book = Book.create("0-8044-2957-X", "Title", "Author", 1, NOW);
        assertThat(book.getIsbn()).isEqualTo("080442957X");
    }

    @Test
    void assignedIdIsAValidUuid() {
        Book book = Book.create("9780132350884", "Title", "Author", 1, NOW);
        assertThat(UUID.fromString(book.getId().toString())).isEqualTo(book.getId());
    }
}
