package com.example.elibrary.identity.security;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "elibrary.security")
public record SecurityProperties(
        String mode,
        String jwtSecret,
        String issuer,
        String issuerUri,
        Duration tokenTtl,
        List<String> corsAllowedOrigins) {

    public SecurityProperties {
        mode = (mode == null || mode.isBlank()) ? "hmac" : mode.trim().toLowerCase(Locale.ROOT);
        issuer = (issuer == null || issuer.isBlank()) ? "elibrary-local" : issuer.trim();
        tokenTtl = tokenTtl == null ? Duration.ofHours(8) : tokenTtl;
        corsAllowedOrigins = corsAllowedOrigins == null ? List.of() : List.copyOf(corsAllowedOrigins);
    }

    public boolean hmacMode() {
        return "hmac".equals(mode);
    }

    public boolean issuerMode() {
        return "issuer".equals(mode);
    }
}
