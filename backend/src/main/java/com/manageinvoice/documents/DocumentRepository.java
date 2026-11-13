package com.manageinvoice.documents;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    Page<Document> findByOwnerIdAndDeletedAtIsNull(UUID ownerId, Pageable pageable);
    Optional<Document> findByIdAndOwnerIdAndDeletedAtIsNull(UUID id, UUID ownerId);
    Optional<Document> findByOwnerIdAndChecksumAndDeletedAtIsNull(UUID ownerId, String checksum);
}
