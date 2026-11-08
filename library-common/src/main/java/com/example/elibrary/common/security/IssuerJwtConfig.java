package com.example.elibrary.common.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * OIDC/JWKS mode for production. The service only consumes tokens; it never issues them.
 */
@Configuration
@ConditionalOnProperty(prefix = "elibrary.security", name = "mode", havingValue = "issuer")
class IssuerJwtConfig {

    private static final OAuth2TokenValidator<Jwt> SUBJECT_VALIDATOR = jwt -> {
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "The token has no subject", null));
        }
        return OAuth2TokenValidatorResult.success();
    };

    @Bean
    JwtDecoder jwtDecoder(SecurityProperties properties) {
        String issuerUri = properties.issuerUri();
        if (issuerUri == null || issuerUri.isBlank()) {
            throw new IllegalStateException(
                    "elibrary.security.issuer-uri is required when security mode is 'issuer'");
        }
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuerUri), SUBJECT_VALIDATOR));
        return decoder;
    }
}
