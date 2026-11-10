package com.saasstarter.documents;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "storage.local.root=target/test-private-storage")
class DocumentApiTests {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

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

    private String register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "email", email, "password", "SecurePass123", "firstName", "Test", "lastName", "User"))))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("accessToken").asText();
    }

    private MockMultipartFile pdf(String filename) {
        return new MockMultipartFile("file", filename, MediaType.APPLICATION_PDF_VALUE,
            "%PDF-1.7\nsample invoice".getBytes(StandardCharsets.UTF_8));
    }
}
