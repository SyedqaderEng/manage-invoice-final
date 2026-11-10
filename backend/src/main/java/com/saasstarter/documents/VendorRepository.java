package com.saasstarter.documents;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorRepository extends JpaRepository<Vendor, UUID> {
    List<Vendor> findByOwnerIdOrderByDisplayNameAsc(UUID ownerId);
    boolean existsByOwnerIdAndNormalizedName(UUID ownerId, String normalizedName);
}
