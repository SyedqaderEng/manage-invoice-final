package com.manageinvoice.documents;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "document_ocr_text")
class DocumentOcrText {
    @Id
    @Column(name = "document_id")
    private UUID documentId;

    @Column(name = "extracted_text", nullable = false, columnDefinition = "text")
    private String extractedText;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected DocumentOcrText() {}

    DocumentOcrText(UUID documentId, String extractedText) {
        this.documentId = documentId;
        this.extractedText = extractedText;
        this.processedAt = Instant.now();
    }

    String getExtractedText() { return extractedText; }
    Instant getProcessedAt() { return processedAt; }
}
