package com.example.elibrary.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.elibrary.catalog.domain.Book;
import com.example.elibrary.support.AbstractPostgresIT;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class LibraryApiIT extends AbstractPostgresIT {

    @Test
    void borrowAndReturnLifecycle() {
        Book book = createBook("Clean Code", 2);
        String token = userToken("alice");

        ResponseEntity<String> borrow =
                postJson("/api/v1/loans", token, Map.of("bookId", book.getId()));

        assertThat(borrow.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode loan = json(borrow);
        String loanId = loan.path("id").asText();
        assertThat(borrow.getHeaders().getFirst(HttpHeaders.LOCATION))
                .isEqualTo("/api/v1/loans/" + loanId);
        assertThat(loan.path("status").asText()).isEqualTo("ACTIVE");
        assertThat(loan.path("bookTitle").asText()).isEqualTo("Clean Code");

        Instant borrowedAt = Instant.parse(loan.path("borrowedAt").asText());
        Instant dueAt = Instant.parse(loan.path("dueAt").asText());
        assertThat(dueAt).isEqualTo(borrowedAt.plus(Duration.ofDays(14)));

        JsonNode myLoans = json(get("/api/v1/me/loans?status=ACTIVE", token));
        assertThat(myLoans.path("totalElements").asLong()).isEqualTo(1);
        assertThat(myLoans.path("items").get(0).path("id").asText()).isEqualTo(loanId);

        JsonNode bookWhileBorrowed = json(get("/api/v1/books/" + book.getId(), token));
        assertThat(bookWhileBorrowed.path("availableCopies").asInt()).isEqualTo(1);

        ResponseEntity<String> returned = post("/api/v1/loans/" + loanId + "/return", token);
        assertThat(returned.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json(returned).path("status").asText()).isEqualTo("RETURNED");
        assertThat(json(returned).path("returnedAt").isTextual()).isTrue();

        JsonNode bookAfterReturn = json(get("/api/v1/books/" + book.getId(), token));
        assertThat(bookAfterReturn.path("availableCopies").asInt()).isEqualTo(2);
    }

    @Test
    void returnIsIdempotent() {
        Book book = createBook("Refactoring", 1);
        String token = userToken("alice");
        String loanId = borrow(token, book.getId());

        ResponseEntity<String> first = post("/api/v1/loans/" + loanId + "/return", token);
        ResponseEntity<String> second = post("/api/v1/loans/" + loanId + "/return", token);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json(second).path("id").asText()).isEqualTo(loanId);
        assertThat(json(second).path("status").asText()).isEqualTo("RETURNED");
        assertThat(json(second).path("returnedAt").asText())
                .isEqualTo(json(first).path("returnedAt").asText());

        JsonNode active = json(get("/api/v1/me/loans?status=ACTIVE", token));
        assertThat(active.path("totalElements").asLong()).isZero();
    }

    @Test
    void cannotBorrowBeyondTotalCopies() {
        Book book = createBook("Effective Java", 1);

        ResponseEntity<String> first =
                postJson("/api/v1/loans", userToken("alice"), Map.of("bookId", book.getId()));
        ResponseEntity<String> second =
                postJson("/api/v1/loans", userToken("bob"), Map.of("bookId", book.getId()));

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(errorCode(second)).isEqualTo("BOOK_NOT_AVAILABLE");

        JsonNode bookView = json(get("/api/v1/books/" + book.getId(), userToken("alice")));
        assertThat(bookView.path("availableCopies").asInt()).isZero();
    }

    @Test
    void sameUserCannotBorrowSameBookTwice() {
        Book book = createBook("DDD", 1);
        String token = userToken("alice");
        borrow(token, book.getId());

        ResponseEntity<String> duplicate =
                postJson("/api/v1/loans", token, Map.of("bookId", book.getId()));

        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(errorCode(duplicate)).isEqualTo("LOAN_ALREADY_ACTIVE");
    }

    @Test
    void ownerAndAdminAuthorizationIsEnforced() {
        Book book = createBook("SRE", 1);
        String alice = userToken("alice");
        String bob = userToken("bob");
        String admin = adminToken("admin");
        String loanId = borrow(alice, book.getId());

        ResponseEntity<String> bobView = get("/api/v1/loans/" + loanId, bob);
        ResponseEntity<String> adminView = get("/api/v1/loans/" + loanId, admin);
        ResponseEntity<String> bobReturn = post("/api/v1/loans/" + loanId + "/return", bob);
        ResponseEntity<String> adminReturn = post("/api/v1/loans/" + loanId + "/return", admin);

        assertThat(bobView.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(errorCode(bobView)).isEqualTo("ACCESS_DENIED");
        assertThat(adminView.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(bobReturn.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(adminReturn.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void identityComesFromTokenNotFromRequestBody() {
        Book book = createBook("Pragmatic Programmer", 2);
        String alice = userToken("alice");
        String mallory = userToken("mallory");

        ResponseEntity<String> borrow = postJson(
                "/api/v1/loans",
                alice,
                Map.of("bookId", book.getId(), "userId", "mallory"));

        assertThat(borrow.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(json(get("/api/v1/me/loans", alice)).path("totalElements").asLong())
                .isEqualTo(1);
        assertThat(json(get("/api/v1/me/loans", mallory)).path("totalElements").asLong())
                .isZero();
    }

    @Test
    void authenticationAndRoleChecksAreEnforced() {
        ResponseEntity<String> anonymous = get("/api/v1/me/loans", null);
        ResponseEntity<String> invalid = get("/api/v1/me/loans", "not-a-jwt");
        ResponseEntity<String> wrongRole =
                get("/api/v1/books", tokenWithRoles("auditor", "AUDITOR"));

        assertThat(anonymous.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(anonymous.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE)).isNotNull();
        assertThat(errorCode(anonymous)).isEqualTo("UNAUTHORIZED");
        assertThat(invalid.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(wrongRole.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(errorCode(wrongRole)).isEqualTo("ACCESS_DENIED");
    }

    @Test
    void validationAndErrorContractAreStable() {
        String token = userToken("alice");
        Book book = createBook("Validation", 1);

        ResponseEntity<String> tooLarge = get("/api/v1/books?size=51", token);
        ResponseEntity<String> invalidStatus = get("/api/v1/me/loans?status=OVERDUE", token);
        ResponseEntity<String> missingBody = postJson("/api/v1/loans", token, Map.of());
        ResponseEntity<String> invalidBook =
                post("/api/v1/loans/" + UUID.randomUUID() + "/return", token);
        ResponseEntity<String> unknownBook = get("/api/v1/books/" + UUID.randomUUID(), token);
        ResponseEntity<String> unknownLoan = post("/api/v1/loans/" + UUID.randomUUID() + "/return", token);

        assertThat(tooLarge.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(errorCode(tooLarge)).isEqualTo("VALIDATION_FAILED");
        assertThat(json(tooLarge).path("errors").get(0).path("field").asText()).isEqualTo("size");
        assertThat(tooLarge.getHeaders().getContentType())
                .isNotNull();
        assertThat(tooLarge.getHeaders().getContentType().toString())
                .startsWith("application/problem+json");

        assertThat(invalidStatus.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(errorCode(invalidStatus)).isEqualTo("MALFORMED_REQUEST");
        assertThat(missingBody.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(errorCode(missingBody)).isEqualTo("VALIDATION_FAILED");
        assertThat(invalidBook.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(errorCode(invalidBook)).isEqualTo("LOAN_NOT_FOUND");
        assertThat(unknownBook.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(errorCode(unknownBook)).isEqualTo("BOOK_NOT_FOUND");
        assertThat(unknownLoan.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(unknownLoan.getHeaders().getFirst("X-Trace-Id")).isNotBlank();
        assertThat(json(unknownLoan).path("traceId").asText()).isNotBlank();

        ResponseEntity<String> malformed = exchange(
                "/api/v1/loans", org.springframework.http.HttpMethod.POST, token, "{broken");
        assertThat(malformed.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(errorCode(malformed)).isEqualTo("MALFORMED_REQUEST");

        assertThat(book.getId()).isNotNull();
    }

    @LocalServerPort
    private int port;

    @Test
    void corsAllowsBrowserOriginsIncludingLanAddress() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        for (String origin : List.of(
                "http://localhost:8088",
                "http://127.0.0.1:8088",
                "http://192.168.68.108:8088")) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:" + port + "/api/v1/books"))
                    .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                    .header("Origin", origin)
                    .header("Access-Control-Request-Method", "POST")
                    .header("Access-Control-Request-Headers", "authorization,content-type")
                    .build();

            HttpResponse<Void> response =
                    client.send(request, HttpResponse.BodyHandlers.discarding());

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).contains(origin);
        }
    }

    @Test
    void searchIsCaseInsensitiveAndPagesAreStable() {
        createBook("Clean Code", 1);
        createBook("Domain-Driven Design", 1);
        createBook("Refactoring", 1);
        String token = userToken("alice");

        JsonNode search = json(get("/api/v1/books?q=CLEAN&page=0&size=10", token));
        assertThat(search.path("totalElements").asLong()).isEqualTo(1);
        assertThat(search.path("items").get(0).path("title").asText()).isEqualTo("Clean Code");

        JsonNode all = json(get("/api/v1/books?page=0&size=2", token));
        assertThat(all.path("items").size()).isEqualTo(2);
        assertThat(all.path("totalElements").asLong()).isEqualTo(3);
        assertThat(all.path("totalPages").asInt()).isEqualTo(2);
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void concurrentBorrowsNeverExceedCapacity() throws Exception {
        Book book = createBook("Concurrent Borrowing", 3);
        int attempts = 20;
        List<String> tokens = new ArrayList<>();
        for (int index = 0; index < attempts; index++) {
            tokens.add(userToken("concurrent-user-" + index));
        }

        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<BorrowResult>> futures = new ArrayList<>();
        try {
            for (String token : tokens) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await(30, TimeUnit.SECONDS);
                    ResponseEntity<String> response =
                            postJson("/api/v1/loans", token, Map.of("bookId", book.getId()));
                    String code = response.getStatusCode().is2xxSuccessful()
                            ? ""
                            : json(response).path("code").asText();
                    return new BorrowResult(response.getStatusCode().value(), code);
                }));
            }
            assertThat(ready.await(30, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<BorrowResult> results = new ArrayList<>();
            for (Future<BorrowResult> future : futures) {
                results.add(future.get(60, TimeUnit.SECONDS));
            }

            assertThat(results.stream().filter(result -> result.status() == 201).count())
                    .isEqualTo(3);
            assertThat(results.stream().filter(result -> result.status() == 409).count())
                    .isEqualTo(17);
            assertThat(results.stream()
                            .filter(result -> result.status() == 409)
                            .allMatch(result -> "BOOK_NOT_AVAILABLE".equals(result.code())))
                    .isTrue();
            assertThat(loanRepository.countActiveByBook(book.getId())).isEqualTo(3);
            assertThat(json(get("/api/v1/books/" + book.getId(), tokens.get(0)))
                            .path("availableCopies")
                            .asInt())
                    .isZero();
        } finally {
            executor.shutdownNow();
        }
    }

    private record BorrowResult(int status, String code) {
    }
}
