package com.manageinvoice.documents;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.jdbc.core.JdbcTemplate;
import com.manageinvoice.testsupport.SupabaseDatabaseTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@SupabaseDatabaseTest
class DocumentApiTests {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;
    @MockitoSpyBean ObjectStorage storage;
    @MockitoSpyBean DocumentRepository documents;

    @Test
    void documentsArePrivateToTheirOwnerAndCanBeDeleted() throws Exception {
        String ownerToken = register("document-owner-" + UUID.randomUUID() + "@example.com");
        String otherToken = register("document-other-" + UUID.randomUUID() + "@example.com");
        MockMultipartFile file = pdf("../../invoice.pdf");

        MvcResult upload = mockMvc.perform(multipart("/api/v1/documents")
                .file(file)
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.filename").value("invoice.pdf"))
            .andReturn();
        String id = objectMapper.readTree(upload.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/v1/documents").header("Authorization", "Bearer " + otherToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/v1/documents/{id}/content", id).header("Authorization", "Bearer " + otherToken))
            .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/documents/{id}/download", id).header("Authorization", "Bearer " + otherToken))
            .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/documents/{id}", id).header("Authorization", "Bearer " + otherToken))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/documents/{id}/content", id).header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(content().bytes(file.getBytes()));
        mockMvc.perform(get("/api/v1/documents/{id}/download", id).header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
            .andExpect(content().bytes(file.getBytes()));
        mockMvc.perform(delete("/api/v1/documents/{id}", id).header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isNoContent());
    }

    @Test
    void uploadValidatesFileSignatureAndRejectsDuplicateContent() throws Exception {
        String token = register("document-validation-" + UUID.randomUUID() + "@example.com");
        mockMvc.perform(multipart("/api/v1/documents")
                .file(new MockMultipartFile("file", "not-a-pdf.pdf", MediaType.APPLICATION_PDF_VALUE,
                    "not a pdf".getBytes(StandardCharsets.UTF_8)))
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isUnsupportedMediaType());

        mockMvc.perform(multipart("/api/v1/documents")
                .file(pdf("first.pdf"))
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated());
        mockMvc.perform(multipart("/api/v1/documents")
                .file(pdf("duplicate.pdf"))
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isConflict());
        mockMvc.perform(get("/api/v1/billing/personal/usage").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.trackedUploads").value(1))
            .andExpect(jsonPath("$.historyComplete").value(true))
            .andExpect(jsonPath("$.limitEnforced").value(false));
    }

    @Test
    void usageIsAccountScopedPersistsAfterDeletionAndDoesNotEnforceLimitYet() throws Exception {
        String owner = register("usage-owner-" + UUID.randomUUID() + "@example.com");
        String other = register("usage-other-" + UUID.randomUUID() + "@example.com");
        mockMvc.perform(get("/api/v1/billing/personal/usage"))
            .andExpect(status().is4xxClientError());
        for (int i = 0; i < 31; i++) {
            MockMultipartFile file = new MockMultipartFile("file", "receipt.pdf", MediaType.APPLICATION_PDF_VALUE,
                ("%PDF-1.7\nreceipt " + i).getBytes(StandardCharsets.UTF_8));
            MvcResult result = mockMvc.perform(multipart("/api/v1/documents").file(file)
                    .header("Authorization", "Bearer " + owner))
                .andExpect(status().isCreated()).andReturn();
            if (i == 29) {
                mockMvc.perform(get("/api/v1/billing/personal/usage").header("Authorization", "Bearer " + owner))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.trackedUploads").value(30));
            }
            String id = objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asText();
            mockMvc.perform(delete("/api/v1/documents/{id}", id).header("Authorization", "Bearer " + owner))
                .andExpect(status().isNoContent());
        }
        mockMvc.perform(get("/api/v1/billing/personal/usage").header("Authorization", "Bearer " + owner))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.trackedUploads").value(31))
            .andExpect(jsonPath("$.limitEnforced").value(false));
        mockMvc.perform(get("/api/v1/billing/personal/usage").header("Authorization", "Bearer " + other))
            .andExpect(status().isOk()).andExpect(jsonPath("$.trackedUploads").value(0));
    }

    @Test
    void paginationReturnsEveryOwnedDocumentInStableOrderWhenUploadTimesMatch() throws Exception {
        String owner = register("paging-owner-" + UUID.randomUUID() + "@example.com");
        String other = register("paging-other-" + UUID.randomUUID() + "@example.com");
        var ids = new HashSet<String>();
        for (int i = 0; i < 21; i++) {
            var file = new MockMultipartFile("file", "page-" + i + ".pdf", MediaType.APPLICATION_PDF_VALUE,
                ("%PDF-1.7\npage " + i).getBytes(StandardCharsets.UTF_8));
            MvcResult result = mockMvc.perform(multipart("/api/v1/documents").file(file)
                .header("Authorization", "Bearer " + owner)).andExpect(status().isCreated()).andReturn();
            ids.add(objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asText());
        }
        for (String id : ids) {
            jdbc.update("update ledgerly_test.documents set uploaded_at = ? where id = ?",
                java.sql.Timestamp.from(java.time.Instant.parse("2026-01-01T00:00:00Z")), UUID.fromString(id));
        }
        var found = new HashSet<String>();
        for (int page = 0; page < 2; page++) {
            MvcResult result = mockMvc.perform(get("/api/v1/documents").param("page", String.valueOf(page)).param("size", "20")
                .header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(21)).andReturn();
            JsonNode contents = objectMapper.readTree(result.getResponse().getContentAsString()).path("content");
            assertEquals(page == 0 ? 20 : 1, contents.size());
            for (JsonNode document : contents) found.add(document.path("id").asText());
        }
        assertEquals(ids, found);
        mockMvc.perform(get("/api/v1/documents").header("Authorization", "Bearer " + other))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        for (String id : ids) {
            mockMvc.perform(delete("/api/v1/documents/{id}", id).header("Authorization", "Bearer " + owner))
                .andExpect(status().isNoContent());
        }
    }

    @Test
    void storageAndDatabaseFailuresDoNotConsumeUsage() throws Exception {
        String owner = register("usage-failure-" + UUID.randomUUID() + "@example.com");
        doThrow(new IOException("storage unavailable")).when(storage).store(any(), any(), any());
        mockMvc.perform(multipart("/api/v1/documents").file(pdf("failure.pdf"))
                .header("Authorization", "Bearer " + owner))
            .andExpect(status().isInternalServerError());
        mockMvc.perform(get("/api/v1/billing/personal/usage").header("Authorization", "Bearer " + owner))
            .andExpect(status().isOk()).andExpect(jsonPath("$.trackedUploads").value(0));
    }

    @Test
    void databaseFailureRollsBackUsageAndRemovesStoredObject() throws Exception {
        String owner = register("usage-db-failure-" + UUID.randomUUID() + "@example.com");
        doThrow(new IllegalStateException("database unavailable")).when(documents).saveAndFlush(any(Document.class));
        assertThrows(Exception.class, () -> mockMvc.perform(multipart("/api/v1/documents").file(pdf("failure.pdf"))
            .header("Authorization", "Bearer " + owner)));
        verify(storage).delete(any());
        mockMvc.perform(get("/api/v1/billing/personal/usage").header("Authorization", "Bearer " + owner))
            .andExpect(status().isOk()).andExpect(jsonPath("$.trackedUploads").value(0));
    }

    @Test
    void concurrentUploadsAreCountedWithoutLostUpdates() throws Exception {
        String owner = register("usage-concurrent-" + UUID.randomUUID() + "@example.com");
        int count = 8;
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(count)) {
            var results = new ArrayList<java.util.concurrent.Future<MvcResult>>();
            for (int i = 0; i < count; i++) {
                byte[] content = ("%PDF-1.7\nconcurrent " + i).getBytes(StandardCharsets.UTF_8);
                results.add(executor.submit(() -> {
                    if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out waiting to start");
                    return mockMvc.perform(multipart("/api/v1/documents")
                        .file(new MockMultipartFile("file", "receipt.pdf", MediaType.APPLICATION_PDF_VALUE, content))
                        .header("Authorization", "Bearer " + owner)).andReturn();
                }));
            }
            start.countDown();
            for (var result : results) {
                MvcResult response = result.get(30, TimeUnit.SECONDS);
                assertEquals(201, response.getResponse().getStatus());
                String id = objectMapper.readTree(response.getResponse().getContentAsString()).path("id").asText();
                mockMvc.perform(delete("/api/v1/documents/{id}", id).header("Authorization", "Bearer " + owner))
                    .andExpect(status().isNoContent());
            }
        }
        mockMvc.perform(get("/api/v1/billing/personal/usage").header("Authorization", "Bearer " + owner))
            .andExpect(status().isOk()).andExpect(jsonPath("$.trackedUploads").value(count));
    }

    @Test
    void vendorsAndCategoriesAreAccountScoped() throws Exception {
        String ownerToken = register("customer-data-owner-" + UUID.randomUUID() + "@example.com");
        String otherToken = register("customer-data-other-" + UUID.randomUUID() + "@example.com");
        MvcResult categoryResult = mockMvc.perform(post("/api/v1/categories")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", "Household", "color", "#336699"))))
            .andExpect(status().isCreated())
            .andReturn();
        String categoryId = objectMapper.readTree(categoryResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/v1/vendors")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "displayName", "Local Market", "defaultCategoryId", categoryId))))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/categories").header("Authorization", "Bearer " + otherToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/v1/vendors").header("Authorization", "Bearer " + otherToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(post("/api/v1/vendors")
                .header("Authorization", "Bearer " + otherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "displayName", "Unrelated Market", "defaultCategoryId", categoryId))))
            .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/categories")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", " household "))))
            .andExpect(status().isConflict());
    }

    @Test
    void customerDataCanBeEditedAndDeactivatedWithoutCrossAccountAccess() throws Exception {
        String owner = register("editing-owner-" + UUID.randomUUID() + "@example.com");
        String other = register("editing-other-" + UUID.randomUUID() + "@example.com");
        MvcResult createdCategory = mockMvc.perform(post("/api/v1/categories")
                .header("Authorization", "Bearer " + owner).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", "Groceries"))))
            .andExpect(status().isCreated()).andReturn();
        String categoryId = objectMapper.readTree(createdCategory.getResponse().getContentAsString()).get("id").asText();
        MvcResult createdVendor = mockMvc.perform(post("/api/v1/vendors")
                .header("Authorization", "Bearer " + owner).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("displayName", "Market", "defaultCategoryId", categoryId))))
            .andExpect(status().isCreated()).andReturn();
        String vendorId = objectMapper.readTree(createdVendor.getResponse().getContentAsString()).get("id").asText();
        String vendorUpdate = objectMapper.writeValueAsString(Map.of("displayName", "New Market"));
        String categoryUpdate = objectMapper.writeValueAsString(Map.of("name", "Food"));

        mockMvc.perform(patch("/api/v1/vendors/{id}", vendorId).header("Authorization", "Bearer " + other)
                .contentType(MediaType.APPLICATION_JSON).content(vendorUpdate)).andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/categories/{id}", categoryId).header("Authorization", "Bearer " + other))
            .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/categories/{id}", categoryId).header("Authorization", "Bearer " + owner))
            .andExpect(status().isConflict());
        mockMvc.perform(patch("/api/v1/categories/{id}", categoryId).header("Authorization", "Bearer " + owner)
                .contentType(MediaType.APPLICATION_JSON).content(categoryUpdate))
            .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Food"));
        mockMvc.perform(patch("/api/v1/vendors/{id}", vendorId).header("Authorization", "Bearer " + owner)
                .contentType(MediaType.APPLICATION_JSON).content(vendorUpdate))
            .andExpect(status().isOk()).andExpect(jsonPath("$.displayName").value("New Market"));
        mockMvc.perform(delete("/api/v1/categories/{id}", categoryId).header("Authorization", "Bearer " + owner))
            .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/vendors/{id}", vendorId).header("Authorization", "Bearer " + owner))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/categories").header("Authorization", "Bearer " + owner))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/v1/vendors").header("Authorization", "Bearer " + owner))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    private String register(String email) {
        return com.manageinvoice.common.security.TestSupabaseTokenVerifier.tokenFor(email);
    }

    private MockMultipartFile pdf(String filename) {
        return new MockMultipartFile("file", filename, MediaType.APPLICATION_PDF_VALUE,
            "%PDF-1.7\nsample invoice".getBytes(StandardCharsets.UTF_8));
    }
}
