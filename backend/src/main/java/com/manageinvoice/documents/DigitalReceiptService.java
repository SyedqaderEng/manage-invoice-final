package com.manageinvoice.documents;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manageinvoice.identity.UserRepository;

@Service
class DigitalReceiptService {
    private final DocumentService documents;
    private final DocumentOcrService ocr;
    private final ReceiptExtractor extractor;
    private final LocalVisionReceiptExtractor vision;
    private final boolean aiEnabled;
    private final DigitalReceiptRepository records;
    private final UserRepository users;
    private final CategoryRepository categories;
    private final ObjectMapper json;
    private final TransactionTemplate transactions;

    DigitalReceiptService(DocumentService documents, DocumentOcrService ocr, ReceiptExtractor extractor,
                          DigitalReceiptRepository records, UserRepository users, ObjectMapper json,
                          LocalVisionReceiptExtractor vision, @Value("${receipt.ai.enabled:false}") boolean aiEnabled,
                          TransactionTemplate transactions, CategoryRepository categories) {
        this.documents = documents;
        this.ocr = ocr;
        this.extractor = extractor;
        this.vision = vision;
        this.aiEnabled = aiEnabled;
        this.records = records;
        this.users = users;
        this.json = json;
        this.transactions = transactions;
        this.categories = categories;
    }

    @Transactional(readOnly = true)
    ReceiptResponse get(UUID ownerId, UUID documentId) {
        documents.findOwned(ownerId, documentId);
        return response(records.findById(documentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No digital record exists yet.")));
    }

    @Transactional(readOnly = true)
    Page<SummaryRecord> confirmed(UUID ownerId, int page) {
        if (page < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid page");
        return records.findConfirmedByOwner(ownerId, PageRequest.of(page, 100)).map(row -> {
            DigitalReceiptRecord record = (DigitalReceiptRecord) row[0];
            Document document = (Document) row[1];
            ReceiptData data = response(record).data();
            String category = data.categoryId() == null ? null : categories.findByIdAndOwnerId(data.categoryId(), ownerId)
                .map(Category::getName).orElse(null);
            return new SummaryRecord(document.getId(), document.getOriginalFilename(), category, data);
        });
    }

    ReceiptResponse create(UUID ownerId, UUID documentId, boolean recognize) {
        documents.findOwned(ownerId, documentId);
        if (records.existsById(documentId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A digital record already exists. Open it instead.");
        }
        ReceiptData data;
        if (!recognize) {
            data = new ReceiptData(null, null, null, null, null, null, null, null, null, null, List.of());
        } else {
            data = suggest(ownerId, documentId);
        }
        ReceiptResponse saved = transactions.execute(status -> {
            lockOwner(ownerId);
            documents.findOwned(ownerId, documentId);
            if (records.existsById(documentId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A digital record already exists. Open it instead.");
            }
            return response(records.saveAndFlush(new DigitalReceiptRecord(documentId, serialize(data))));
        });
        if (saved == null) throw new IllegalStateException("Digital record creation returned no result");
        return saved;
    }

    ReceiptData suggest(UUID ownerId, UUID documentId) {
        DocumentService.OwnedDocument owned = documents.findOwned(ownerId, documentId);
        return aiEnabled ? vision.extract(documents.read(owned), owned.document().getMimeType())
            : extractor.extract(ocr.process(ownerId, documentId).text());
    }

    @Transactional
    ReceiptResponse save(UUID ownerId, UUID documentId, SaveRequest request) {
        lockOwner(ownerId);
        documents.findOwned(ownerId, documentId);
        DigitalReceiptRecord record = records.findById(documentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No digital record exists yet."));
        if (request.version() != record.getVersion()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This record changed elsewhere. Reload it before saving.");
        }
        UUID categoryId = request.data().categoryId();
        if (categoryId != null) {
            boolean unchanged = categoryId.equals(response(record).data().categoryId());
            boolean valid = unchanged ? categories.findByIdAndOwnerId(categoryId, ownerId).isPresent()
                : categories.findByIdAndOwnerIdAndActiveTrue(categoryId, ownerId).isPresent();
            if (!valid) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found for this account");
        }
        record.update(serialize(request.data()), request.confirmed());
        return response(records.saveAndFlush(record));
    }

    private void lockOwner(UUID ownerId) {
        users.findByIdForUpdate(ownerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found"));
    }

    private String serialize(ReceiptData data) {
        try {
            return json.writeValueAsString(data);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Digital record serialization failed", exception);
        }
    }

    private ReceiptResponse response(DigitalReceiptRecord record) {
        try {
            return new ReceiptResponse(record.getStatus(), record.getVersion(), record.getUpdatedAt(),
                json.readValue(record.getDataJson(), ReceiptData.class));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Saved digital record is invalid", exception);
        }
    }

    record SaveRequest(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Long version,
                       boolean confirmed, @jakarta.validation.constraints.NotNull @jakarta.validation.Valid ReceiptData data) {}
    record ReceiptResponse(String status, long version, Instant updatedAt, ReceiptData data) {}
    record SummaryRecord(UUID documentId, String filename, String category, ReceiptData data) {}
}
