package com.example.elibrary.shared.api;

import com.example.elibrary.shared.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public final class ProblemDetailsFactory {

    private static final String PROBLEM_TYPE_BASE = "https://elibrary.example/problems/";

    private ProblemDetailsFactory() {
    }

    public static ProblemDetail create(
            ErrorCode errorCode, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.valueOf(errorCode.httpStatus()), detail);
        problem.setType(URI.create(PROBLEM_TYPE_BASE + errorCode.slug()));
        problem.setTitle(errorCode.title());
        problem.setProperty("code", errorCode.name());
        problem.setProperty("traceId", TraceContext.currentTraceId());
        if (request != null) {
            problem.setInstance(URI.create(request.getRequestURI()));
        }
        return problem;
    }
}
