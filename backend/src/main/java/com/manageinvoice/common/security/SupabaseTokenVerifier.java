package com.manageinvoice.common.security;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class SupabaseTokenVerifier {
    private final RestClient client;
    private final ObjectMapper json;
    private final String publishableKey;

    @Autowired
    public SupabaseTokenVerifier(@Value("${security.supabase.url}") String url,
                                 @Value("${security.supabase.publishable-key}") String publishableKey,
                                 ObjectMapper json) {
        URI uri = URI.create(url);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
            || !uri.getHost().endsWith(".supabase.co") || uri.getUserInfo() != null
            || uri.getQuery() != null || uri.getFragment() != null
            || (!uri.getPath().isEmpty() && !"/".equals(uri.getPath()))
            || (uri.getPort() != -1 && uri.getPort() != 443)) {
            throw new IllegalArgumentException("Invalid Supabase project URL");
        }
        if (publishableKey.isBlank()) {
            throw new IllegalArgumentException("Supabase publishable key is required");
        }
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.client = RestClient.builder().baseUrl(url.endsWith("/") ? url.substring(0, url.length() - 1) : url)
            .requestFactory(factory).build();
        this.publishableKey = publishableKey;
        this.json = json;
    }

    SupabaseTokenVerifier(RestClient client, String publishableKey, ObjectMapper json) {
        this.client = client;
        this.publishableKey = publishableKey;
        this.json = json;
    }

    public Optional<Identity> verify(String token) {
        try {
            return client.get().uri("/auth/v1/user")
                .header("apikey", publishableKey)
                .headers(headers -> headers.setBearerAuth(token))
                .exchange((request, response) -> {
                    if (response.getStatusCode().value() == 401 || response.getStatusCode().value() == 403) {
                        return Optional.empty();
                    }
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        throw new VerificationUnavailableException("Supabase Auth returned an unexpected response");
                    }
                    try {
                        JsonNode user = json.readTree(response.getBody());
                        UUID id = UUID.fromString(user.path("id").asText());
                        String email = user.path("email").asText();
                        String[] parts = token.split("\\.");
                        if (parts.length != 3 || email.isBlank() || email.length() > 320) {
                            throw new IllegalArgumentException("Invalid verified identity");
                        }
                        JsonNode payload = json.readTree(Base64.getUrlDecoder().decode(parts[1]));
                        if (!payload.path("exp").canConvertToLong()) {
                            throw new IllegalArgumentException("Missing token expiration");
                        }
                        Instant expiration = Instant.ofEpochSecond(payload.path("exp").asLong());
                        if (!expiration.isAfter(Instant.now())) {
                            return Optional.empty();
                        }
                        JsonNode metadata = user.path("user_metadata");
                        return Optional.of(new Identity(id, email, name(metadata.path("first_name"), metadata.path("given_name")),
                            name(metadata.path("last_name"), metadata.path("family_name")),
                            displayName(metadata.path("full_name"), metadata.path("name")), expiration));
                    } catch (IOException | IllegalArgumentException ex) {
                        throw new VerificationUnavailableException("Invalid Supabase Auth response", ex);
                    }
                });
        } catch (RestClientException ex) {
            throw new VerificationUnavailableException("Supabase Auth is unavailable", ex);
        }
    }

    private static String name(JsonNode value, JsonNode fallback) {
        String text = value.isTextual() ? value.asText().trim() : "";
        if (text.isEmpty() && fallback.isTextual()) text = fallback.asText().trim();
        return text.length() <= 100 ? text : text.substring(0, 100);
    }

    private static String displayName(JsonNode value, JsonNode fallback) {
        String text = value.isTextual() ? value.asText().trim() : "";
        if (text.isEmpty() && fallback.isTextual()) text = fallback.asText().trim();
        return text.length() <= 200 ? text : text.substring(0, 200);
    }

    public record Identity(UUID id, String email, String firstName, String lastName, String displayName, Instant expiresAt) {}

    public static class VerificationUnavailableException extends RuntimeException {
        VerificationUnavailableException(String message) { super(message); }
        VerificationUnavailableException(String message, Throwable cause) { super(message, cause); }
    }
}
