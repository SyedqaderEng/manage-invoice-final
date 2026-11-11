package com.manageinvoice.documents;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "digital_receipt_records")
class DigitalReceiptRecord {
    @Id
    @Column(name = "document_id")
    private UUID documentId;
    @Column(nullable = false, length = 16)
    private String status;
    @Column(name = "data_json", nullable = false, columnDefinition = "text")
    private String dataJson;
    @Version
    private long version;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DigitalReceiptRecord() {}

    DigitalReceiptRecord(UUID documentId, String dataJson) {
        this.documentId = documentId;
        this.status = "DRAFT";
        this.dataJson = dataJson;
        this.updatedAt = Instant.now();
    }

    void update(String dataJson, boolean confirmed) {
        this.dataJson = dataJson;
        this.status = confirmed ? "CONFIRMED" : "DRAFT";
        this.updatedAt = Instant.now();
    }

    String getStatus() { return status; }
    String getDataJson() { return dataJson; }
    long getVersion() { return version; }
    Instant getUpdatedAt() { return updatedAt; }
}
