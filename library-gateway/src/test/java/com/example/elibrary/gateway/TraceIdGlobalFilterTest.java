package com.example.elibrary.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

class TraceIdGlobalFilterTest {

    private final TraceIdGlobalFilter filter = new TraceIdGlobalFilter();

    @Test
    void preservesValidIncomingTraceId() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/books")
                        .header(TraceIdGlobalFilter.HEADER, "trace-123")
                        .build());
        CapturingChain chain = new CapturingChain();

        filter.filter(exchange, chain).block();
        exchange.getResponse().setComplete().block();

        assertThat(exchange.getResponse().getHeaders().get(TraceIdGlobalFilter.HEADER))
                .containsExactly("trace-123");
        assertThat(chain.exchange.getRequest().getHeaders().getFirst(TraceIdGlobalFilter.HEADER))
                .isEqualTo("trace-123");
    }

    @Test
    void replacesInvalidIncomingTraceId() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/books")
                        .header(TraceIdGlobalFilter.HEADER, "bad value with spaces")
                        .build());
        CapturingChain chain = new CapturingChain();

        filter.filter(exchange, chain).block();
        exchange.getResponse().setComplete().block();

        String traceId = exchange.getResponse().getHeaders().getFirst(TraceIdGlobalFilter.HEADER);
        assertThat(traceId).isNotBlank().doesNotContain(" ");
        assertThat(chain.exchange.getRequest().getHeaders().getFirst(TraceIdGlobalFilter.HEADER))
                .isEqualTo(traceId);
    }

    private static final class CapturingChain implements GatewayFilterChain {
        private ServerWebExchange exchange;

        @Override
        public Mono<Void> filter(ServerWebExchange exchange) {
            this.exchange = exchange;
            return Mono.empty();
        }
    }
}
