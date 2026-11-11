package com.manageinvoice.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

class SupabaseTokenVerifierTests {
    @Test
    void obtainsIdentityFromTheVerifiedAuthResponse() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://example.supabase.co");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var verifier = new SupabaseTokenVerifier(builder.build(), "public-test-key", new ObjectMapper());
        UUID id = UUID.randomUUID();
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
            ("{\"exp\":" + Instant.now().plusSeconds(600).getEpochSecond() + "}").getBytes(StandardCharsets.UTF_8));
        String token = "header." + payload + ".signature";
        server.expect(requestTo("https://example.supabase.co/auth/v1/user"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("apikey", "public-test-key"))
            .andExpect(header("Authorization", "Bearer " + token))
            .andRespond(withSuccess("{\"id\":\"" + id + "\",\"email\":\"test@example.com\","
                + "\"user_metadata\":{\"first_name\":\"Test\",\"last_name\":\"User\"}}", MediaType.APPLICATION_JSON));
        var identity = verifier.verify(token).orElseThrow();
        assertEquals(id, identity.id());
        assertEquals("Test", identity.firstName());
        server.verify();
    }

    @Test
    void distinguishesInvalidCredentialsFromProviderFailure() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://example.supabase.co");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var verifier = new SupabaseTokenVerifier(builder.build(), "public-test-key", new ObjectMapper());
        server.expect(requestTo("https://example.supabase.co/auth/v1/user"))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        server.expect(requestTo("https://example.supabase.co/auth/v1/user"))
            .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        assertFalse(verifier.verify("bad").isPresent());
        assertThrows(SupabaseTokenVerifier.VerificationUnavailableException.class, () -> verifier.verify("bad"));
        server.verify();
    }
}
