package com.saasstarter.billing;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(properties = "billing.stripe.webhook-secret=test_webhook_secret")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BillingWebhookTests {
    private static final String WEBHOOK_SECRET = "test_webhook_secret";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void acceptsSignedSubscriptionEventAndRejectsInvalidSignature() throws Exception {
        String token = register();
        JsonNode workspace = createWorkspace(token);
        String workspaceId = workspace.get("id").asText();
        String payload = """
            {
              "id":"evt_subscription_created",
              "type":"customer.subscription.created",
              "data":{"object":{
                "id":"sub_test_123",
                "customer":"cus_test_123",
                "status":"active",
                "current_period_end":1893456000,
                "metadata":{"workspace_id":"%s","plan_id":"pro","interval":"monthly"}
              }}
            }
            """.formatted(workspaceId);

        mockMvc.perform(post("/api/v1/billing/webhook")
                .header("Stripe-Signature", signature(payload))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/billing/workspaces/{workspaceId}", workspaceId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.plan").value("pro"))
            .andExpect(jsonPath("$.status").value("active"));

        mockMvc.perform(post("/api/v1/billing/webhook")
                .header("Stripe-Signature", "t=" + Instant.now().getEpochSecond() + ",v1=deadbeef")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload.replace("evt_subscription_created", "evt_invalid_signature")))
            .andExpect(status().isBadRequest());
    }

    private String register() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "email", "billing-owner@example.com", "password", "SecurePass123",
                    "firstName", "Billing", "lastName", "Owner"))))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private JsonNode createWorkspace(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/workspaces")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Billing Team","workspaceType":"ORGANIZATION"}
                    """))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String signature(String payload) throws Exception {
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String digest = HexFormat.of().formatHex(mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8)));
        return "t=" + timestamp + ",v1=" + digest;
    }
}
