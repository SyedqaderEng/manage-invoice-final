package com.manageinvoice.documents;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.UUID;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import com.manageinvoice.testsupport.SupabaseDatabaseTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@SpringBootTest
@AutoConfigureMockMvc
@SupabaseDatabaseTest
class DigitalReceiptRealOcrTests {
    @Autowired MockMvc http;
    @Autowired ObjectMapper json;

    @Test
    void realPdfExtractionCanBeReviewedConfirmedAndReopened() throws Exception {
        String token = com.manageinvoice.common.security.TestSupabaseTokenVerifier.newUserToken();
        byte[] pdfBytes;
        try (PDDocument pdf = new PDDocument(); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            pdf.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(pdf, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 16);
                stream.newLineAtOffset(50, 700);
                for (String line : new String[] {"LOCAL MARKET", "Date: 2026-03-12", "BANANA 2.50", "Total 2.70"}) {
                    stream.showText(line);
                    stream.newLineAtOffset(0, -22);
                }
                stream.endText();
            }
            pdf.save(bytes);
            pdfBytes = bytes.toByteArray();
        }
        var upload = http.perform(multipart("/api/v1/documents")
            .file(new MockMultipartFile("file", "receipt.pdf", "application/pdf", pdfBytes))
            .header("Authorization", "Bearer " + token)).andExpect(status().isCreated()).andReturn();
        String id = json.readTree(upload.getResponse().getContentAsString()).path("id").asText();
        var draft = http.perform(post("/api/v1/documents/{id}/record", id).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.vendor").value("LOCAL MARKET"))
            .andExpect(jsonPath("$.data.total").value(2.70))
            .andExpect(jsonPath("$.data.items[0].originalDescription").value("BANANA")).andReturn();
        var parsed = json.readTree(draft.getResponse().getContentAsString());
        ObjectNode data = (ObjectNode) parsed.path("data");
        ((ObjectNode) data.withArray("items").get(0)).put("description", "Bananas").put("verified", true);
        http.perform(put("/api/v1/documents/{id}/record", id).header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(
                Map.of("version", parsed.path("version").asLong(), "confirmed", true, "data", data))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMED"));
        http.perform(get("/api/v1/documents/{id}/record", id).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].description").value("Bananas"));
        http.perform(delete("/api/v1/documents/{id}", id).header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());
    }
}
