package com.manageinvoice.common.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * In-process stand-in for Supabase Auth. Tokens created by {@link #tokenFor(String)} are accepted
 * and map to a stable user id per email address; any other token is rejected.
 */
public class TestSupabaseTokenVerifier extends SupabaseTokenVerifier {
    private static final String PREFIX = "test-token:";

    public TestSupabaseTokenVerifier() {
        super((org.springframework.web.client.RestClient) null, "test-publishable-key", new ObjectMapper());
    }

    /** Returns a bearer token that this verifier accepts as the given (new or existing) user. */
    public static String tokenFor(String email) {
        return PREFIX + email;
    }

    /** Returns a bearer token for a brand-new random user. */
    public static String newUserToken() {
        return tokenFor(UUID.randomUUID() + "@example.com");
    }

    public static UUID userIdFor(String email) {
        return UUID.nameUUIDFromBytes(email.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Optional<Identity> verify(String token) {
        if (token == null || !token.startsWith(PREFIX) || token.length() == PREFIX.length()) return Optional.empty();
        String email = token.substring(PREFIX.length());
        return Optional.of(new Identity(userIdFor(email), email, "Test", "User", Instant.now().plusSeconds(3600)));
    }
}
