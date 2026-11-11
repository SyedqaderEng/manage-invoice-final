package com.manageinvoice.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import com.manageinvoice.testsupport.SupabaseDatabaseTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.fasterxml.jackson.databind.JsonNode;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@SupabaseDatabaseTest
class SupabaseAuthHttpTests {
    @Autowired TestRestTemplate http;
    @MockitoBean SupabaseTokenVerifier verifier;

    @Test
    void verifiedIdentityIsProvisionedAndRevokedWithoutAcceptingLocalLogin() {
        UUID id = UUID.randomUUID();
        String email = id + "@example.com";
        var identity = new SupabaseTokenVerifier.Identity(id, email, "Test", "Person", Instant.now().plusSeconds(3600));
        when(verifier.verify(anyString())).thenReturn(Optional.empty());
        when(verifier.verify("verified-token")).thenReturn(Optional.of(identity));

        assertTrue(authenticated("/api/v1/auth/me", HttpMethod.GET, "forged-token").getStatusCode().is4xxClientError());
        var account = authenticated("/api/v1/auth/me", HttpMethod.GET, "verified-token");
        assertEquals(HttpStatus.OK, account.getStatusCode());
        assertEquals(id.toString(), account.getBody().path("id").asText());
        assertEquals("Test", account.getBody().path("firstName").asText());
        assertEquals(HttpStatus.OK, authenticated("/api/v1/auth/logout", HttpMethod.POST, "verified-token").getStatusCode());
        assertTrue(authenticated("/api/v1/auth/me", HttpMethod.GET, "verified-token").getStatusCode().is4xxClientError());
    }

    @Test
    void localPasswordLoginNoLongerExists() {
        var localLogin = http.postForEntity("/api/v1/auth/login",
            Map.of("email", "someone@example.com", "password", "anything"), JsonNode.class);
        assertTrue(localLogin.getStatusCode().is4xxClientError());
    }

    @Test
    void providerFailureIsNotReportedAsAnInvalidSession() {
        when(verifier.verify("unavailable-token")).thenThrow(
            new SupabaseTokenVerifier.VerificationUnavailableException("Test provider unavailable"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,
            authenticated("/api/v1/auth/me", HttpMethod.GET, "unavailable-token").getStatusCode());
    }

    @Test
    void sameEmailDoesNotLinkDifferentSupabaseAccounts() {
        String email = UUID.randomUUID() + "@example.com";
        when(verifier.verify("first-identity")).thenReturn(Optional.of(new SupabaseTokenVerifier.Identity(
            UUID.randomUUID(), email, "First", "Person", Instant.now().plusSeconds(3600))));
        when(verifier.verify("second-identity")).thenReturn(Optional.of(new SupabaseTokenVerifier.Identity(
            UUID.randomUUID(), email, "Second", "Person", Instant.now().plusSeconds(3600))));
        assertEquals(HttpStatus.OK, authenticated("/api/v1/auth/me", HttpMethod.GET, "first-identity").getStatusCode());
        assertEquals(HttpStatus.CONFLICT, authenticated("/api/v1/auth/me", HttpMethod.GET, "second-identity").getStatusCode());
    }

    private org.springframework.http.ResponseEntity<JsonNode> authenticated(String path, HttpMethod method, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return http.exchange(path, method, new HttpEntity<>(headers), JsonNode.class);
    }
}
