package com.manageinvoice.documents;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface DocumentOcrTextRepository extends JpaRepository<DocumentOcrText, UUID> {}
