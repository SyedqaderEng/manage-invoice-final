package com.manageinvoice.documents;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.manageinvoice.identity.User;
import com.manageinvoice.identity.UserRepository;
import com.manageinvoice.billing.PersonalUsageService;

@Service
public class DocumentService {
    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
    public static final long MAX_FILE_SIZE = 25L * 1024 * 1024;
    private final DocumentRepository documents;
    private final UserRepository users;
    private final ObjectStorage storage;
    private final DocumentOcrTextRepository ocrTexts;
    private final PersonalUsageService usage;
    private final DigitalReceiptRepository receipts;

    public DocumentService(DocumentRepository documents, UserRepository users, ObjectStorage storage, DocumentOcrTextRepository ocrTexts, PersonalUsageService usage, DigitalReceiptRepository receipts) {
        this.documents = documents;
        this.users = users;
        this.storage = storage;
        this.ocrTexts = ocrTexts;
        this.usage = usage;
        this.receipts = receipts;
    }

    @Transactional(readOnly = true)
    public Page<DocumentResponse> list(UUID ownerId, Pageable pageable) {
        Pageable boundedPage = PageRequest.of(
            pageable.getPageNumber(),
            Math.min(pageable.getPageSize(), 100),
            Sort.by(Sort.Direction.DESC, "uploadedAt", "id")
        );
        return documents.findByOwnerIdAndDeletedAtIsNull(ownerId, boundedPage).map(DocumentResponse::new);
    }

    @Transactional
    public DocumentResponse upload(UUID ownerId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a document to upload");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Documents must be 25 MB or smaller");
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The selected document could not be read");
        }
        String mimeType = detectMimeType(content);
        if (mimeType == null) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Upload a PDF, JPEG, PNG, or WebP document");
        }
        String filename = cleanFilename(file.getOriginalFilename());
        String checksum = checksum(content);
        User owner = users.findByIdForUpdate(ownerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found"));
        if (documents.existsByOwnerIdAndChecksumAndDeletedAtIsNull(ownerId, checksum)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This document has already been uploaded");
        }
        usage.recordUpload(ownerId);

        Document document = new Document();
        UUID documentId = UUID.randomUUID();
        document.setId(documentId);
        document.setOwner(owner);
        document.setUploadedBy(owner);
        document.setOriginalFilename(filename);
        document.setMimeType(mimeType);
        document.setFileSize(content.length);
        document.setChecksum(checksum);
        document.setStorageKey(ownerId + "/" + documentId);
        try {
            storage.store(ownerId, documentId, content);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "The document could not be stored securely", exception);
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    try {
                        storage.delete(document.getStorageKey());
                    } catch (IOException exception) {
                        log.error("Failed to remove document object after transaction rollback: {}", documentId, exception);
                    }
                } else if (status == STATUS_UNKNOWN) {
                    log.error("Document transaction outcome is unknown; object requires reconciliation: {}", documentId);
                }
            }
        });
        return new DocumentResponse(documents.saveAndFlush(document));
    }

    @Transactional(readOnly = true)
    public OwnedDocument findOwned(UUID ownerId, UUID documentId) {
        Document document = documents.findByIdAndOwnerIdAndDeletedAtIsNull(documentId, ownerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        return new OwnedDocument(document);
    }

    public byte[] read(OwnedDocument owned) {
        try {
            return storage.read(owned.document().getStorageKey());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "The document could not be read securely", exception);
        }
    }

    @Transactional
    public void delete(UUID ownerId, UUID documentId) {
        users.findByIdForUpdate(ownerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found"));
        Document document = documents.findByIdAndOwnerIdAndDeletedAtIsNull(documentId, ownerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        try {
            storage.delete(document.getStorageKey());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "The document could not be deleted securely", exception);
        }
        ocrTexts.deleteById(documentId);
        receipts.deleteById(documentId);
        documents.delete(document);
    }

    private String cleanFilename(String value) {
        String filename = value == null ? "" : value.replace('\\', '/');
        int separator = filename.lastIndexOf('/');
        filename = filename.substring(separator + 1).replaceAll("[\\p{Cntrl}]", "").trim();
        if (filename.isBlank()) filename = "document";
        if (filename.length() > 255) {
            int end = Character.isHighSurrogate(filename.charAt(254)) ? 254 : 255;
            filename = filename.substring(0, end);
        }
        return filename;
    }

    private String detectMimeType(byte[] content) {
        if (startsWith(content, new byte[] {'%', 'P', 'D', 'F', '-'})) return "application/pdf";
        if (startsWith(content, new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff})) return "image/jpeg";
        if (startsWith(content, new byte[] {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10})) return "image/png";
        if (content.length >= 12
            && startsWith(content, new byte[] {'R', 'I', 'F', 'F'})
            && new String(content, 8, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP")) {
            return "image/webp";
        }
        return null;
    }

    private boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (value[i] != prefix[i]) return false;
        }
        return true;
    }

    private String checksum(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record OwnedDocument(Document document) {}
}
