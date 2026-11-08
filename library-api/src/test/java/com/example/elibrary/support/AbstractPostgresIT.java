package com.example.elibrary.support;

import com.example.elibrary.catalog.domain.Book;
import com.example.elibrary.catalog.domain.BookRepository;
import com.example.elibrary.common.security.JwtTokenIssuer;
import com.example.elibrary.loan.domain.LoanRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Integration tests run against a real PostgreSQL instance because SQLite cannot exercise row
 * locking or multi-connection concurrency. The container is started once per test JVM and the
 * schema is created by the same Flyway migrations used in production.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractPostgresIT {

    private static final AtomicInteger ISBN_SEQUENCE = new AtomicInteger(1_000_000);

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("elibrary")
                    .withUsername("elibrary")
                    .withPassword("elibrary");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void registerDatabaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    protected TestRestTemplate rest;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JwtTokenIssuer tokenIssuer;

    @Autowired
    protected BookRepository bookRepository;

    @Autowired
    protected LoanRepository loanRepository;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected CacheManager cacheManager;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE loan, book CASCADE");
        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }
    }

    protected Book createBook(String title, int totalCopies) {
        String isbn = "978" + String.format("%010d", ISBN_SEQUENCE.incrementAndGet());
        Book book = Book.create(isbn, title, "Test Author", totalCopies, Instant.now());
        return bookRepository.save(book);
    }

    protected String userToken(String userId) {
        return tokenIssuer.issue(userId, Set.of("USER")).token();
    }

    protected String adminToken(String userId) {
        return tokenIssuer.issue(userId, Set.of("ADMIN")).token();
    }

    protected String tokenWithRoles(String userId, String... roles) {
        return tokenIssuer.issue(userId, Set.of(roles)).token();
    }

    protected ResponseEntity<String> get(String path, String token) {
        return exchange(path, HttpMethod.GET, token, null);
    }

    protected ResponseEntity<String> post(String path, String token) {
        return exchange(path, HttpMethod.POST, token, null);
    }

    protected ResponseEntity<String> postJson(String path, String token, Object body) {
        return exchange(path, HttpMethod.POST, token, body);
    }

    protected ResponseEntity<String> exchange(
            String path, HttpMethod method, String token, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON, MediaType.APPLICATION_PROBLEM_JSON));
        if (token != null) {
            headers.setBearerAuth(token);
        }
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        return rest.exchange(path, method, new HttpEntity<>(body, headers), String.class);
    }

    protected JsonNode json(ResponseEntity<String> response) {
        try {
            return objectMapper.readTree(response.getBody());
        } catch (Exception exception) {
            throw new AssertionError("Response body is not valid JSON: " + response.getBody(), exception);
        }
    }

    protected String errorCode(ResponseEntity<String> response) {
        return json(response).path("code").asText();
    }

    protected String borrow(String token, UUID bookId) {
        ResponseEntity<String> response =
                postJson("/api/v1/loans", token, Map.of("bookId", bookId));
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new AssertionError(
                    "Borrow failed: " + response.getStatusCode() + " " + response.getBody());
        }
        return json(response).path("id").asText();
    }
}
