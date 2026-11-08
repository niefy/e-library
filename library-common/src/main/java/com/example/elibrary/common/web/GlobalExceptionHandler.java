package com.example.elibrary.common.web;

import com.example.elibrary.common.error.DomainException;
import com.example.elibrary.common.error.ErrorCode;
import com.example.elibrary.common.error.ProblemDetailsFactory;
import com.example.elibrary.common.trace.TraceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetail> handleDomain(
            DomainException exception, HttpServletRequest request) {
        return response(exception.errorCode(), exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleBodyValidation(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<Map<String, String>> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", Optional.ofNullable(error.getDefaultMessage()).orElse("invalid")))
                .toList();
        return validationResponse(errors, request);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ProblemDetail> handleMethodValidation(
            HandlerMethodValidationException exception, HttpServletRequest request) {
        List<Map<String, String>> errors = new ArrayList<>();
        exception.getAllValidationResults().forEach(result -> {
            String field = result.getMethodParameter().getParameterName();
            String fieldName = field == null ? "parameter" : field;
            result.getResolvableErrors().forEach(error -> errors.add(Map.of(
                    "field", fieldName,
                    "message", Optional.ofNullable(error.getDefaultMessage()).orElse("invalid"))));
        });
        return validationResponse(errors, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(
            ConstraintViolationException exception, HttpServletRequest request) {
        List<Map<String, String>> errors = exception.getConstraintViolations().stream()
                .map(violation -> Map.of(
                        "field", simpleFieldName(violation.getPropertyPath().toString()),
                        "message", violation.getMessage()))
                .toList();
        return validationResponse(errors, request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleMissingParameter(
            MissingServletRequestParameterException exception, HttpServletRequest request) {
        return response(
                ErrorCode.VALIDATION_FAILED,
                "Required parameter '%s' is missing".formatted(exception.getParameterName()),
                request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        return response(
                ErrorCode.MALFORMED_REQUEST,
                "Parameter '%s' has an invalid value".formatted(exception.getName()),
                request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadableBody(
            HttpMessageNotReadableException exception, HttpServletRequest request) {
        return response(ErrorCode.MALFORMED_REQUEST, "Request body is missing or malformed", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(
            IllegalArgumentException exception, HttpServletRequest request) {
        return response(ErrorCode.VALIDATION_FAILED, "Request parameters are invalid", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResource(
            NoResourceFoundException exception, HttpServletRequest request) {
        return response(ErrorCode.RESOURCE_NOT_FOUND, "The requested resource was not found", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
        return response(ErrorCode.METHOD_NOT_ALLOWED, "HTTP method is not supported", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(
            AccessDeniedException exception, HttpServletRequest request) {
        return response(ErrorCode.ACCESS_DENIED, "You are not allowed to perform this action", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthentication(
            AuthenticationException exception, HttpServletRequest request) {
        return response(ErrorCode.UNAUTHORIZED, "Authentication is required", request);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLock(
            OptimisticLockingFailureException exception, HttpServletRequest request) {
        return response(
                ErrorCode.CONCURRENT_MODIFICATION,
                "The resource changed concurrently; retry the request",
                request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleIntegrityViolation(
            DataIntegrityViolationException exception, HttpServletRequest request) {
        log.warn("Unmapped data integrity violation traceId={}", TraceContext.currentTraceId(), exception);
        return response(ErrorCode.DATA_CONFLICT, "Request conflicts with existing data", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(
            Exception exception, HttpServletRequest request) {
        log.error("Unhandled exception traceId={}", TraceContext.currentTraceId(), exception);
        return response(
                ErrorCode.INTERNAL_ERROR, "An unexpected error occurred", request);
    }

    private static String simpleFieldName(String propertyPath) {
        int lastDot = propertyPath.lastIndexOf('.');
        return lastDot < 0 ? propertyPath : propertyPath.substring(lastDot + 1);
    }

    private static ResponseEntity<ProblemDetail> validationResponse(
            List<Map<String, String>> errors, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetailsFactory.create(
                ErrorCode.VALIDATION_FAILED, "Request validation failed", request);
        problem.setProperty("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private static ResponseEntity<ProblemDetail> response(
            ErrorCode errorCode, String detail, HttpServletRequest request) {
        return ResponseEntity.status(errorCode.httpStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(ProblemDetailsFactory.create(errorCode, detail, request));
    }
}
