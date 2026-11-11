package com.manageinvoice.documents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import com.manageinvoice.testsupport.SupabaseDatabaseTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@SpringBootTest
@AutoConfigureMockMvc
@SupabaseDatabaseTest
class DigitalReceiptTests {
    @Autowired MockMvc http;
    @Autowired ObjectMapper json;
    @MockitoBean LocalOcr ocr;

    @Test
    void parsesWalmartStyleReceiptText() {
        // Text mirrors walmart-reciept.png as OCR would return it.
        ReceiptData result = new ReceiptExtractor().extract(String.join("\n",
            "See back of receipt for your chance", "to win $1000", "ID #: 7ZXXZ523GBA", "Walmart",
            "Save money. Live better.", "(916) 968-2258", "MANAGER RENE PUENTES", "3451 TRUXEL RD",
            "SACRAMENTO CA 23456", "STORE # 23", "CASHIER 567890", "PRODUCT SERIAL # TH66B3C1ZZ",
            "HP ENVY 4512 012345678976 $69.00", "SUBTOTAL $69.00", "TAX (8%) $5.52", "TOTAL $74.52",
            "DEBIT TEND 74.05", "CHANGE DUE 0.00"));
        assertEquals("Walmart", result.vendor());
        assertEquals(new BigDecimal("69.00"), result.subtotal());
        assertEquals(new BigDecimal("5.52"), result.tax());
        assertEquals(new BigDecimal("74.52"), result.total());
        assertEquals("USD", result.currency());
        assertEquals(1, result.items().size());
        assertEquals("HP ENVY 4512", result.items().get(0).originalDescription());
        assertEquals(new BigDecimal("69.00"), result.items().get(0).lineTotal());
    }

    @Test
    void extractsConservativelyWithoutInventingMissingValues() {
        ReceiptData result = new ReceiptExtractor().extract(
            "LOCAL MARKET\nDate: 2026-03-12\nReceipt #: A123\nBANANA 2.50\nSubtotal 2.50\nTax 0.20\nTotal 2.70");
        assertEquals("LOCAL MARKET", result.vendor());
        assertEquals("A123", result.receiptNumber());
        assertEquals(new BigDecimal("2.70"), result.total());
        assertNull(result.currency());
        assertNull(result.discount());
        assertEquals("BANANA", result.items().get(0).originalDescription());
        assertEquals("BANANA", result.items().get(0).description());
        assertEquals(false, result.items().get(0).verified());
        ReceiptData detailed = new ReceiptExtractor().extract(
            "SHOP\nDate: 12/24/2025\nTime: 14:32\nCurrency: USD\nAPPLE 2 x 1.50 3.00\nTotal 3.00");
        assertEquals(java.time.LocalDate.of(2025, 12, 24), detailed.transactionDate());
        assertEquals("14:32", detailed.transactionTime());
        assertEquals("USD", detailed.currency());
        assertEquals(new BigDecimal("2"), detailed.items().get(0).quantity());
        assertEquals(new BigDecimal("1.50"), detailed.items().get(0).unitPrice());
        assertNull(new ReceiptExtractor().extract("SHOP\nDate: 03/04/2025").transactionDate());
    }

    @Test
    void reviewConfirmPersistConflictIsolationAndDelete() throws Exception {
        String owner = register();
        String other = register();
        String id = upload(owner);
        when(ocr.extract(any(), any())).thenReturn("LOCAL MARKET\nDate: 2026-03-12\nBANANA 2.50\nTotal 2.70");
        http.perform(post("/api/v1/documents/{id}/record", id).header("Authorization", "Bearer " + other))
            .andExpect(status().isNotFound());
        var created = http.perform(post("/api/v1/documents/{id}/record", id).header("Authorization", "Bearer " + owner))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.data.items[0].originalDescription").value("BANANA"))
            .andExpect(jsonPath("$.data.currency").doesNotExist()).andReturn();
        var result = json.readTree(created.getResponse().getContentAsString());
        ObjectNode data = (ObjectNode) result.path("data");
        var ownerCategory = http.perform(post("/api/v1/categories").header("Authorization", "Bearer " + owner)
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Groceries\"}"))
            .andExpect(status().isCreated()).andReturn();
        String categoryId = json.readTree(ownerCategory.getResponse().getContentAsString()).path("id").asText();
        var otherCategory = http.perform(post("/api/v1/categories").header("Authorization", "Bearer " + other)
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Private\"}"))
            .andExpect(status().isCreated()).andReturn();
        data.put("categoryId", json.readTree(otherCategory.getResponse().getContentAsString()).path("id").asText());
        http.perform(put("/api/v1/documents/{id}/record", id).header("Authorization", "Bearer " + owner)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("version", result.path("version").asLong(), "confirmed", true, "data", data))))
            .andExpect(status().isBadRequest());
        data.put("categoryId", categoryId);
        data.put("vendor", "Local Market");
        data.put("currency", "USD");
        ((ObjectNode) data.withArray("items").get(0)).put("description", "Bananas").put("verified", true);
        String request = json.writeValueAsString(Map.of("version", result.path("version").asLong(), "confirmed", true, "data", data));
        http.perform(put("/api/v1/documents/{id}/record", id).header("Authorization", "Bearer " + other)
            .contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isNotFound());
        data.put("total", -1);
        http.perform(put("/api/v1/documents/{id}/record", id).header("Authorization", "Bearer " + owner)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("version", result.path("version").asLong(), "confirmed", true, "data", data))))
            .andExpect(status().isBadRequest());
        data.put("total", 2.70);
        http.perform(put("/api/v1/documents/{id}/record", id).header("Authorization", "Bearer " + owner)
            .contentType(MediaType.APPLICATION_JSON).content(request))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.data.items[0].originalDescription").value("BANANA"))
            .andExpect(jsonPath("$.data.items[0].description").value("Bananas"));
        http.perform(put("/api/v1/documents/{id}/record", id).header("Authorization", "Bearer " + owner)
            .contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isConflict());
        http.perform(post("/api/v1/documents/{id}/record", id).header("Authorization", "Bearer " + owner))
            .andExpect(status().isConflict());
        http.perform(get("/api/v1/documents/{id}/record", id).header("Authorization", "Bearer " + other))
            .andExpect(status().isNotFound());
        http.perform(get("/api/v1/documents/{id}/record", id).header("Authorization", "Bearer " + owner))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.data.vendor").value("Local Market"))
            .andExpect(jsonPath("$.data.categoryId").value(categoryId));
        http.perform(get("/api/v1/documents/records/confirmed").header("Authorization", "Bearer " + owner))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].category").value("Groceries"))
            .andExpect(jsonPath("$.content[0].data.total").value(2.70));
        http.perform(get("/api/v1/documents/records/confirmed").header("Authorization", "Bearer " + other))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(0));
        http.perform(delete("/api/v1/categories/{id}", categoryId).header("Authorization", "Bearer " + owner))
            .andExpect(status().isNoContent());
        http.perform(get("/api/v1/documents/records/confirmed").header("Authorization", "Bearer " + owner))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].category").value("Groceries"));
        http.perform(delete("/api/v1/documents/{id}", id).header("Authorization", "Bearer " + owner))
            .andExpect(status().isNoContent());
        http.perform(get("/api/v1/documents/{id}/record", id).header("Authorization", "Bearer " + owner))
            .andExpect(status().isNotFound());
    }

    @Test
    void failedRecognitionAllowsManualRecordWithoutLosingOriginal() throws Exception {
        String owner = register();
        String id = upload(owner);
        when(ocr.extract(any(), any())).thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "OCR unavailable"));
        http.perform(post("/api/v1/documents/{id}/record", id).header("Authorization", "Bearer " + owner))
            .andExpect(status().isServiceUnavailable());
        http.perform(get("/api/v1/documents/{id}/content", id).header("Authorization", "Bearer " + owner))
            .andExpect(status().isOk());
        http.perform(post("/api/v1/documents/{id}/record?recognize=false", id).header("Authorization", "Bearer " + owner))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.data.items.length()").value(0));
        http.perform(delete("/api/v1/documents/{id}", id).header("Authorization", "Bearer " + owner))
            .andExpect(status().isNoContent());
    }

    private String register() {
        return com.manageinvoice.common.security.TestSupabaseTokenVerifier.newUserToken();
    }

    private String upload(String token) throws Exception {
        var result = http.perform(multipart("/api/v1/documents")
            .file(new MockMultipartFile("file", "receipt.pdf", "application/pdf", "%PDF-1.7\nfake".getBytes()))
            .header("Authorization", "Bearer " + token)).andExpect(status().isCreated()).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).path("id").asText();
    }
}
