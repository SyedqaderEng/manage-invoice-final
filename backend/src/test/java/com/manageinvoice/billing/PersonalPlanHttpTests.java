package com.manageinvoice.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import com.manageinvoice.testsupport.SupabaseDatabaseTest;

import com.fasterxml.jackson.databind.JsonNode;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@SupabaseDatabaseTest
class PersonalPlanHttpTests {
    @Autowired TestRestTemplate http;

    @Test
    void publicPersonalCatalogReportsExactTermsWithoutEnablingCheckout() {
        var response = http.getForEntity("/api/v1/billing/personal/plans", JsonNode.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        var plans = response.getBody();
        assertEquals(2, plans.size());
        var free = plans.get(0);
        var pro = plans.get(1);
        assertEquals("free", free.path("id").asText());
        assertEquals(0, free.path("monthlyCents").asInt());
        assertEquals(30, free.path("lifetimeFreeReceipts").asInt());
        assertFalse(free.path("checkoutAvailable").asBoolean());
        assertEquals("pro", pro.path("id").asText());
        assertEquals(1000, pro.path("monthlyCents").asInt());
        assertEquals(6700, pro.path("yearlyCents").asInt());
        assertEquals("usd", pro.path("currency").asText());
        assertTrue(pro.path("unlimitedReceipts").asBoolean());
        assertEquals(1, pro.path("trialMonths").asInt());
        assertFalse(pro.path("checkoutAvailable").asBoolean());
    }
}
