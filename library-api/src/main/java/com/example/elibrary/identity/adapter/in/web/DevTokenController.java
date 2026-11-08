package com.example.elibrary.identity.adapter.in.web;

import com.example.elibrary.common.security.Role;
import com.example.elibrary.common.security.JwtTokenIssuer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Development-only token mint. It exists so the H5 client and reviewers can run the project in
 * five minutes without an IdP. It is disabled outside the "dev" profile and replaced by OAuth2
 * discovery in production (security mode "issuer").
 */
@RestController
@RequestMapping("/api/v1/dev/tokens")
@Profile("dev")
@ConditionalOnProperty(
        prefix = "elibrary.security",
        name = "mode",
        havingValue = "hmac",
        matchIfMissing = true)
public class DevTokenController {

    private final JwtTokenIssuer tokenIssuer;

    public DevTokenController(JwtTokenIssuer tokenIssuer) {
        this.tokenIssuer = tokenIssuer;
    }

    @PostMapping
    public DevTokenResponse issue(@Valid @RequestBody DevTokenRequest request) {
        JwtTokenIssuer.IssuedToken issued =
                tokenIssuer.issue(request.userId(), Set.of(request.role().name()));
        return new DevTokenResponse(
                issued.token(),
                "Bearer",
                issued.expiresAt(),
                request.userId(),
                List.of(request.role().name()));
    }

    public record DevTokenRequest(
            @NotBlank
            @Pattern(regexp = "[A-Za-z0-9._-]{1,64}", message = "must be a simple user identifier")
            String userId,
            @NotNull Role role) {
    }

    public record DevTokenResponse(
            String token,
            String tokenType,
            Instant expiresAt,
            String userId,
            List<String> roles) {
    }
}
