package com.saasstarter.billing;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "processed_stripe_events")
public class ProcessedStripeEvent {
    @Id
    @Column(name = "stripe_event_id", length = 255)
    private String stripeEventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;

    protected ProcessedStripeEvent() {}

    public ProcessedStripeEvent(String stripeEventId, String eventType) {
        this.stripeEventId = stripeEventId;
        this.eventType = eventType;
    }

    @PrePersist
    void onCreate() {
        processedAt = Instant.now();
    }

    public String getStripeEventId() { return stripeEventId; }
}
