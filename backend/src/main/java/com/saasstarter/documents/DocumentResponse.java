package com.saasstarter.documents;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
    UUID id,
    String filename,
    String mimeType,
    long fileSize,
    String documentType,
    String processingStatus,
    Instant uploadedAt,
    String contentUrl,
    String downloadUrl
) {
    public DocumentResponse(Document document) {
        this(document.getId(), document.getOriginalFilename(), document.getMimeType(), document.getFileSize(),
            document.getDocumentType(), document.getProcessingStatus(), document.getUploadedAt(),
            "/api/v1/documents/" + document.getId() + "/content",
            "/api/v1/documents/" + document.getId() + "/download");
    }
}
