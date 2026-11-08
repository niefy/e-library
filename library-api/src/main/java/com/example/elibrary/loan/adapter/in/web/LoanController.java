package com.example.elibrary.loan.adapter.in.web;

import com.example.elibrary.common.paging.PageQuery;
import com.example.elibrary.common.paging.PageResponse;
import com.example.elibrary.common.paging.PageResult;
import com.example.elibrary.common.security.Actor;
import com.example.elibrary.common.security.CurrentUser;
import com.example.elibrary.loan.application.LoanQueryService;
import com.example.elibrary.loan.application.LoanService;
import com.example.elibrary.loan.application.LoanView;
import com.example.elibrary.loan.domain.LoanStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Validated
@Tag(name = "Loans", description = "Borrow, return and inspect loans")
public class LoanController {

    private final LoanService loanService;
    private final LoanQueryService loanQueryService;

    public LoanController(LoanService loanService, LoanQueryService loanQueryService) {
        this.loanService = loanService;
        this.loanQueryService = loanQueryService;
    }

    @PostMapping("/loans")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Operation(summary = "Borrow a book for the authenticated user")
    public ResponseEntity<LoanResponse> borrow(
            @CurrentUser Actor actor, @Valid @RequestBody BorrowBookRequest request) {
        LoanView loan = loanService.borrow(request.bookId(), actor);
        return ResponseEntity.created(URI.create("/api/v1/loans/" + loan.id()))
                .body(LoanResponse.from(loan));
    }

    @PostMapping("/loans/{id}/return")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Operation(summary = "Return a loan; repeated calls are idempotent")
    public LoanResponse returnLoan(@CurrentUser Actor actor, @PathVariable UUID id) {
        return LoanResponse.from(loanService.returnLoan(id, actor));
    }

    @GetMapping("/loans/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Operation(summary = "Get a loan; owners and admins only")
    public LoanResponse getLoan(@CurrentUser Actor actor, @PathVariable UUID id) {
        return LoanResponse.from(loanQueryService.getLoan(id, actor));
    }

    @GetMapping("/me/loans")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Operation(summary = "List the authenticated user's loans")
    public PageResponse<LoanResponse> myLoans(
            @CurrentUser Actor actor,
            @RequestParam(required = false) LoanStatus status,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "must be greater than or equal to 0")
            int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "must be between 1 and 50")
            @Max(value = 50, message = "must be between 1 and 50")
            int size) {
        PageResult<LoanView> result =
                loanQueryService.myLoans(actor, status, new PageQuery(page, size));
        return PageResponse.from(result, LoanResponse::from);
    }
}
