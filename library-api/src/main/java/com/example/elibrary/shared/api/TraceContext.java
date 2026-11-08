package com.example.elibrary.shared.api;

import java.util.regex.Pattern;
import org.slf4j.MDC;

public final class TraceContext {

    public static final String MDC_KEY = "traceId";
    public static final String HEADER = "X-Trace-Id";
    private static final Pattern VALID_TRACE_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    private TraceContext() {
    }

    public static boolean isValid(String value) {
        return value != null && VALID_TRACE_ID.matcher(value).matches();
    }

    public static String currentTraceId() {
        String value = MDC.get(MDC_KEY);
        return value == null ? "unknown" : value;
    }
}
