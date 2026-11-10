package com.saasstarter.billing;

import java.time.Instant;
import java.util.UUID;

import com.saasstarter.tenancy.Workspace;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "workspace_subscriptions")
public class WorkspaceSubscription {
    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false, unique = true)
    private Workspace workspace;

    @Column(name = "stripe_customer_id", unique = true)
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id", unique = true)
    private String stripeSubscriptionId;

    @Column(name = "plan_id", nullable = false, length = 50)
    private String planId;

    @Column(name = "billing_interval", nullable = false, length = 20)
    private String billingInterval;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "current_period_end")
    private Instant currentPeriodEnd;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        id = id == null ? UUID.randomUUID() : id;
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Workspace getWorkspace() { return workspace; }
    public void setWorkspace(Workspace workspace) { this.workspace = workspace; }
    public String getStripeCustomerId() { return stripeCustomerId; }
    public void setStripeCustomerId(String stripeCustomerId) { this.stripeCustomerId = stripeCustomerId; }
    public String getStripeSubscriptionId() { return stripeSubscriptionId; }
    public void setStripeSubscriptionId(String stripeSubscriptionId) { this.stripeSubscriptionId = stripeSubscriptionId; }
    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }
    public String getBillingInterval() { return billingInterval; }
    public void setBillingInterval(String billingInterval) { this.billingInterval = billingInterval; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCurrentPeriodEnd() { return currentPeriodEnd; }
    public void setCurrentPeriodEnd(Instant currentPeriodEnd) { this.currentPeriodEnd = currentPeriodEnd; }
}
