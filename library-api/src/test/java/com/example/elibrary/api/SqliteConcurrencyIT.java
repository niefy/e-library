package com.example.elibrary.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.elibrary.catalog.domain.Book;
import com.example.elibrary.catalog.domain.BookRepository;
import com.example.elibrary.identity.security.JwtTokenIssuer;
import com.example.elibrary.loan.domain.LoanRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Runs the real application against the file-based SQLite profile used by Docker Compose.
 *
 * <p>This is deliberately separate from the PostgreSQL integration test: SQLite serializes
 * writers differently. In particular a deferred transaction that reads before writing can fail
 * with SQLITE_BUSY_SNAPSHOT under concurrent borrows even when busy_timeout is set. The local
 * profile therefore starts transactions in IMMEDIATE mode. These tests fail if that setting or
 * the database capacity guard regresses.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SqliteConcurrencyIT {

    private static final Path DATA_DIR = createTempDirectory();
    private static final AtomicInteger ISBN_SEQUENCE = new AtomicInteger();

    @DynamicPropertySource
    static void sqliteLocation(DynamicPropertyRegistry registry) {
        registry.add("ELIBRARY_DB_PATH", () -> DATA_DIR.resolve("library.db").toString());
    }

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenIssuer tokenIssuer;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Environment environment;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM loan");
        jdbcTemplate.execute("DELETE FROM book");
    }

    @Test
    void sqliteProfileStartsTransactionsInImmediateMode() {
        assertThat(environment.getProperty("spring.datasource.url"))
                .contains("transaction_mode=IMMEDIATE");
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void concurrentBorrowsNeverExceedCapacityWithoutServerErrors() throws Exception {
        Book book = createBook("SQLite Concurrent Borrowing", 1);
        int attempts = 12;
        List<String> tokens = new ArrayList<>();
        for (int index = 0; index < attempts; index++) {
            tokens.add(tokenIssuer.issue("sqlite-capacity-" + index, Set.of("USER")).token());
        }

        List<BorrowResult> results = borrowConcurrently(tokens, book.getId());

        assertThat(results.stream().filter(result -> result.status() == 201).count()).isEqualTo(1);
        assertThat(results.stream().filter(result -> result.status() == 409).count())
                .isEqualTo(attempts - 1);
        assertThat(results.stream().filter(result -> result.status() >= 500).count()).isZero();
        assertThat(results.stream()
                        .filter(result -> result.status() == 409)
                        .allMatch(result -> "BOOK_NOT_AVAILABLE".equals(result.code())))
                .isTrue();
        assertThat(loanRepository.countActiveByBook(book.getId())).isEqualTo(1);
        assertThat(availableCopies(book.getId(), tokens.get(0))).isZero();
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void concurrentSameUserBorrowsCreateExactlyOneActiveLoan() throws Exception {
        Book book = createBook("SQLite Duplicate Guard", 3);
        String token = tokenIssuer.issue("sqlite-same-user", Set.of("USER")).token();
        List<String> tokens = new ArrayList<>();
        for (int index = 0; index < 12; index++) {
            tokens.add(token);
        }

        List<BorrowResult> results = borrowConcurrently(tokens, book.getId());

        assertThat(results.stream().filter(result -> result.status() == 201).count()).isEqualTo(1);
        assertThat(results.stream().filter(result -> result.status() == 409).count()).isEqualTo(11);
        assertThat(results.stream().filter(result -> result.status() >= 500).count()).isZero();
        assertThat(results.stream()
                        .filter(result -> result.status() == 409)
                        .allMatch(result -> "LOAN_ALREADY_ACTIVE".equals(result.code())))
                .isTrue();
        assertThat(loanRepository.countActiveByBook(book.getId())).isEqualTo(1);
        assertThat(availableCopies(book.getId(), token)).isEqualTo(2);
    }

    private List<BorrowResult> borrowConcurrently(List<String> tokens, UUID bookId)
            throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tokens.size());
        CountDownLatch ready = new CountDownLatch(tokens.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<BorrowResult>> futures = new ArrayList<>();
        try {
            for (String token : tokens) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await(30, TimeUnit.SECONDS);
                    ResponseEntity<String> response = borrow(token, bookId);
                    int status = response.getStatusCode().value();
                    String code = status == 201 ? "" : errorCode(response.getBody());
                    return new BorrowResult(status, code);
                }));
            }
            assertThat(ready.await(30, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<BorrowResult> results = new ArrayList<>();
            for (Future<BorrowResult> future : futures) {
                results.add(future.get(60, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    private ResponseEntity<String> borrow(String token, UUID bookId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.APPLICATION_PROBLEM_JSON));
        return rest.exchange(
                "/api/v1/loans",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("bookId", bookId), headers),
                String.class);
    }

    private int availableCopies(UUID bookId, String token) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<String> response = rest.exchange(
                "/api/v1/books/" + bookId, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        return objectMapper.readTree(response.getBody()).path("availableCopies").asInt();
    }

    private String errorCode(String body) throws Exception {
        JsonNode node = objectMapper.readTree(body);
        return node.path("code").asText();
    }

    private Book createBook(String title, int totalCopies) {
        String isbn = "978" + String.format("%010d", 1_000_000 + ISBN_SEQUENCE.incrementAndGet());
        return bookRepository.save(
                Book.create(isbn, title, "Test Author", totalCopies, Instant.now()));
    }

    private static Path createTempDirectory() {
        try {
            return Files.createTempDirectory("elibrary-sqlite-it-");
        } catch (IOException exception) {
            throw new IllegalStateException("cannot create SQLite test directory", exception);
        }
    }

    private record BorrowResult(int status, String code) {
    }
}
