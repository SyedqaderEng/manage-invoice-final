package com.manageinvoice.documents;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
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

@SpringBootTest
@AutoConfigureMockMvc
@SupabaseDatabaseTest
@TestPropertySource(properties = "receipt.ai.enabled=true")
class AiReceiptApiTests {
    @Autowired MockMvc http;
    @Autowired ObjectMapper json;
    @MockitoBean LocalVisionReceiptExtractor vision;

    @Test
    void aiResultIsSavedAndProviderFailureCanBeRetried() throws Exception {
        String token = com.manageinvoice.common.security.TestSupabaseTokenVerifier.newUserToken();
        var upload = http.perform(multipart("/api/v1/documents")
            .file(new MockMultipartFile("file", "receipt.pdf", "application/pdf", "%PDF-1.7\nfake".getBytes()))
            .header("Authorization", "Bearer " + token)).andExpect(status().isCreated()).andReturn();
        String id = json.readTree(upload.getResponse().getContentAsString()).path("id").asText();
        when(vision.extract(any(), any()))
            .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI unavailable"))
            .thenReturn(new ReceiptData("Market", null, null, null, null, null, null, null, null, null, List.of()));
        http.perform(post("/api/v1/documents/{id}/record", id).header("Authorization", "Bearer " + token))
            .andExpect(status().isServiceUnavailable());
        http.perform(get("/api/v1/documents/{id}/content", id).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
        var draft = http.perform(post("/api/v1/documents/{id}/record", id).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.data.vendor").value("Market")).andReturn();
        var savedRecord = json.readTree(draft.getResponse().getContentAsString());
        http.perform(put("/api/v1/documents/{id}/record", id).header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                "version", savedRecord.path("version").asLong(), "confirmed", true, "data", savedRecord.path("data")))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMED"));
        when(vision.extract(any(), any())).thenReturn(
            new ReceiptData("New suggestion", null, null, null, null, null, null, null, null, null, List.of()));
        http.perform(post("/api/v1/documents/{id}/record/suggestions", id).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.vendor").value("New suggestion"));
        http.perform(get("/api/v1/documents/{id}/record", id).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.data.vendor").value("Market"));
        when(vision.extract(any(), any())).thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI unavailable"));
        http.perform(post("/api/v1/documents/{id}/record/suggestions", id).header("Authorization", "Bearer " + token))
            .andExpect(status().isServiceUnavailable());
        http.perform(get("/api/v1/documents/{id}/record", id).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.data.vendor").value("Market"));
        String otherToken = com.manageinvoice.common.security.TestSupabaseTokenVerifier.newUserToken();
        http.perform(post("/api/v1/documents/{id}/record/suggestions", id).header("Authorization", "Bearer " + otherToken))
            .andExpect(status().isNotFound());
        verify(vision, org.mockito.Mockito.times(4)).extract(any(), any());
        http.perform(delete("/api/v1/documents/{id}", id).header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());
    }
}
