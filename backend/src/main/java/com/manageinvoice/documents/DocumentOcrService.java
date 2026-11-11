package com.manageinvoice.documents;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DocumentOcrService {
    private final DocumentService documents;
    private final DocumentOcrTextRepository texts;
    private final LocalOcr ocr;

    DocumentOcrService(DocumentService documents, DocumentOcrTextRepository texts, LocalOcr ocr) {
        this.documents = documents;
        this.texts = texts;
        this.ocr = ocr;
    }

    @Transactional
    OcrResponse process(UUID ownerId, UUID documentId) {
        DocumentService.OwnedDocument document = documents.findOwned(ownerId, documentId);
        String text = ocr.extract(documents.read(document), document.document().getMimeType());
        DocumentOcrText saved = texts.save(new DocumentOcrText(documentId, text));
        return new OcrResponse(saved.getExtractedText(), saved.getProcessedAt());
    }

    record OcrResponse(String text, Instant processedAt) {}
}
