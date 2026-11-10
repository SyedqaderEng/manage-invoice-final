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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "workspace_members", uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "user_id"}))
public class WorkspaceMember {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WorkspaceRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WorkspaceStatus status = WorkspaceStatus.ACTIVE;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @PrePersist
    void onCreate() {
        id = id == null ? UUID.randomUUID() : id;
        joinedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Workspace getWorkspace() { return workspace; }
    public void setWorkspace(Workspace workspace) { this.workspace = workspace; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public WorkspaceRole getRole() { return role; }
    public void setRole(WorkspaceRole role) { this.role = role; }
    public WorkspaceStatus getStatus() { return status; }
    public void setStatus(WorkspaceStatus status) { this.status = status; }
}
