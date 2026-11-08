package com.example.elibrary.catalog.adapter.in.web;

import com.example.elibrary.catalog.application.BookQueryService;
import com.example.elibrary.catalog.application.BookView;
import com.example.elibrary.shared.api.PageResponse;
import com.example.elibrary.shared.paging.PageQuery;
import com.example.elibrary.shared.paging.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/books")
@Validated
@PreAuthorize("hasAnyRole('USER','ADMIN')")
@Tag(name = "Books", description = "Browse and search the catalog")
public class BookController {

    private final BookQueryService bookQueryService;

    public BookController(BookQueryService bookQueryService) {
        this.bookQueryService = bookQueryService;
    }

    @GetMapping
    @Operation(summary = "Search books by title, author or ISBN")
    public PageResponse<BookResponse> search(
            @RequestParam(required = false)
            @Size(max = 100, message = "must be at most 100 characters")
            String q,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "must be greater than or equal to 0")
            int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "must be between 1 and 50")
            @Max(value = 50, message = "must be between 1 and 50")
            int size) {
        PageResult<BookView> result = bookQueryService.searchBooks(q, new PageQuery(page, size));
        return PageResponse.from(result, BookResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a book and its current availability")
    public BookResponse get(@PathVariable UUID id) {
        return BookResponse.from(bookQueryService.getBook(id));
    }
}
