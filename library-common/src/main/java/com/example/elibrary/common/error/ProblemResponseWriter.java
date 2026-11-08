package com.example.elibrary.common.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;

/**
 * Writes ProblemDetail responses from places that are outside Spring MVC's exception handling,
 * notably the Spring Security entry point and access-denied handler.
 */
public final class ProblemResponseWriter {

    private ProblemResponseWriter() {
    }

    public static void write(
            HttpServletResponse response,
            ObjectMapper objectMapper,
            ErrorCode errorCode,
            String detail,
            HttpServletRequest request)
            throws IOException {
        response.setStatus(errorCode.httpStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(
                response.getOutputStream(),
                ProblemDetailsFactory.create(errorCode, detail, request));
    }
}
