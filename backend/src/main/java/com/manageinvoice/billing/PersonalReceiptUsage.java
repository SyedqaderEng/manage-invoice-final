package com.manageinvoice.billing;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "personal_receipt_usage")
public class PersonalReceiptUsage {
    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "tracked_uploads", nullable = false)
    private long trackedUploads;

    @Column(name = "history_complete", nullable = false)
    private boolean historyComplete;

    protected PersonalReceiptUsage() {}

    public PersonalReceiptUsage(UUID userId) {
        this.userId = userId;
        this.historyComplete = true;
    }

    public long getTrackedUploads() { return trackedUploads; }
    public boolean isHistoryComplete() { return historyComplete; }
    public void recordUpload() { trackedUploads = Math.addExact(trackedUploads, 1); }
}
