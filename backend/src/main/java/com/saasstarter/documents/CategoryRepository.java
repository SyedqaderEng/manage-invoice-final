package com.saasstarter.documents;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findByOwnerIdAndActiveTrueOrderByNameAsc(UUID ownerId);
    Optional<Category> findByIdAndOwnerIdAndActiveTrue(UUID id, UUID ownerId);
    boolean existsByOwnerIdAndNameIgnoreCase(UUID ownerId, String name);
}
