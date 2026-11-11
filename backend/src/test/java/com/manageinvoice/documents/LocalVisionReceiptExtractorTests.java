package com.manageinvoice.documents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

import jakarta.validation.Validation;

class LocalVisionReceiptExtractorTests {
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private final jakarta.validation.Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void sendsOnlyToLocalServerAndReturnsUnverifiedItems() throws Exception {
        var request = new AtomicReference<String>();
        String output = """
            {"vendor":"Market","receiptNumber":null,"transactionDate":null,"transactionTime":null,
             "currency":null,"subtotal":2.5,"tax":null,"discount":null,"total":2.5,"paymentInfo":null,"suggestedCategory":"Groceries",
             "items":[{"originalDescription":"BANANA","description":"Invented","verified":true,
               "quantity":null,"unitPrice":null,"lineTotal":2.5}]}
            """;
        var server = server(request, json.writeValueAsString(java.util.Map.of("response", output, "done", true, "done_reason", "stop")), 200);
        try {
            ReceiptData result = new LocalVisionReceiptExtractor(server.getAddress().getPort(), "local:3b", json, validator)
                .extract(new byte[] {1, 2, 3}, "image/png");
            assertEquals("Market", result.vendor());
            assertEquals("Groceries", result.suggestedCategory());
            assertNull(result.categoryId());
            assertEquals("BANANA", result.items().get(0).description());
            assertFalse(result.items().get(0).verified());
            assertNull(result.tax());
            assertTrue(request.get().contains("AQID"));
            assertTrue(request.get().contains("untrusted"));
        } finally { server.stop(0); }
    }

    @Test
    void rejectsTruncatedMalformedAndInvalidResults() throws Exception {
        var request = new AtomicReference<String>();
        for (String output : new String[] {
            "{\"response\":\"{}\",\"done\":true,\"done_reason\":\"stop\"}",
            "{\"response\":\"{}\",\"done\":true,\"done_reason\":\"length\"}",
            "{\"response\":\"not json\",\"done\":true,\"done_reason\":\"stop\"}"
        }) {
            var server = server(request, output, 200);
            try {
                var ex = assertThrows(ResponseStatusException.class, () ->
                    new LocalVisionReceiptExtractor(server.getAddress().getPort(), "local:3b", json, validator)
                        .extract(new byte[] {1}, "image/png"));
                assertTrue(ex.getStatusCode().is5xxServerError());
            } finally { server.stop(0); }
        }
        assertThrows(IllegalArgumentException.class, () -> new LocalVisionReceiptExtractor(11434, "model:cloud", json, validator));
    }

    @Test
    void providerFailureIsNotReportedAsSuccess() throws Exception {
        var server = server(new AtomicReference<>(), "unavailable", 503);
        try {
            var ex = assertThrows(ResponseStatusException.class, () ->
                new LocalVisionReceiptExtractor(server.getAddress().getPort(), "local:3b", json, validator)
                    .extract(new byte[] {1}, "image/png"));
            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
        } finally { server.stop(0); }
    }

    private HttpServer server(AtomicReference<String> request, String body, int status) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/generate", exchange -> {
            request.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (var stream = exchange.getResponseBody()) { stream.write(bytes); }
        });
        server.start();
        return server;
    }
}
