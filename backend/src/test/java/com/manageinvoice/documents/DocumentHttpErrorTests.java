package com.manageinvoice.documents;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
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

import com.fasterxml.jackson.databind.JsonNode;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@SupabaseDatabaseTest
class DocumentHttpErrorTests {
    @Autowired TestRestTemplate http;

    @Test
    void missingPrivateDocumentRetainsNotFoundStatusThroughErrorDispatch() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(com.manageinvoice.common.security.TestSupabaseTokenVerifier.newUserToken());

        var response = http.exchange("/api/v1/documents/" + UUID.randomUUID() + "/content",
            HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, http.getForEntity("/error", String.class).getStatusCode());
    }
}
