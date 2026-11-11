package com.manageinvoice.documents;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorRepository extends JpaRepository<Vendor, UUID> {
    List<Vendor> findByOwnerIdAndActiveTrueOrderByDisplayNameAsc(UUID ownerId);
    Optional<Vendor> findByIdAndOwnerIdAndActiveTrue(UUID id, UUID ownerId);
    boolean existsByOwnerIdAndNormalizedName(UUID ownerId, String normalizedName);
    boolean existsByOwnerIdAndNormalizedNameAndIdNot(UUID ownerId, String normalizedName, UUID id);
    boolean existsByOwnerIdAndDefaultCategoryIdAndActiveTrue(UUID ownerId, UUID categoryId);
}
