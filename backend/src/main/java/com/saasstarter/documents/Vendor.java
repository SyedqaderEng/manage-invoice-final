package com.saasstarter.documents;

import java.time.Instant;
import java.util.UUID;

import com.saasstarter.identity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "vendors")
public class Vendor {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    @Column(name = "normalized_name", nullable = false, length = 255)
    private String normalizedName;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Column(length = 320)
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(columnDefinition = "text")
    private String address;

    @Column(name = "tax_identifier_encrypted", columnDefinition = "text")
    private String taxIdentifierEncrypted;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_category_id")
    private Category defaultCategory;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (id == null) id = UUID.randomUUID();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    public String getNormalizedName() { return normalizedName; }
    public void setNormalizedName(String normalizedName) { this.normalizedName = normalizedName; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Category getDefaultCategory() { return defaultCategory; }
    public void setDefaultCategory(Category defaultCategory) { this.defaultCategory = defaultCategory; }
}
