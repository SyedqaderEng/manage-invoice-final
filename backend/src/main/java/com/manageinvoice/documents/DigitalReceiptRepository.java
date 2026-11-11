package com.manageinvoice.documents;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

interface DigitalReceiptRepository extends JpaRepository<DigitalReceiptRecord, UUID> {
    @Query("select r, d from DigitalReceiptRecord r, Document d where r.documentId = d.id and d.owner.id = :ownerId and d.deletedAt is null and r.status = 'CONFIRMED' order by d.uploadedAt desc, d.id desc")
    Page<Object[]> findConfirmedByOwner(UUID ownerId, Pageable pageable);
}
