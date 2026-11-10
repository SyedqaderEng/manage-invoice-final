package com.saasstarter.tenancy;

import java.time.Instant;
import java.util.UUID;

import com.saasstarter.identity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "workspaces")
public class Workspace {
    @Id
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "workspace_type", nullable = false, length = 30)
    private WorkspaceType workspaceType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    @Column(name = "default_currency", nullable = false, length = 3)
    private String defaultCurrency = "USD";

    @Column(nullable = false, length = 100)
    private String timezone = "UTC";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WorkspaceStatus status = WorkspaceStatus.ACTIVE;

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
    void onUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public WorkspaceType getWorkspaceType() { return workspaceType; }
    public void setWorkspaceType(WorkspaceType workspaceType) { this.workspaceType = workspaceType; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    public String getDefaultCurrency() { return defaultCurrency; }
    public void setDefaultCurrency(String defaultCurrency) { this.defaultCurrency = defaultCurrency; }
}
