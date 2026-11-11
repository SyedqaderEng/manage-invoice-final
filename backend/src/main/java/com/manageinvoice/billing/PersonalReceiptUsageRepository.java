package com.manageinvoice.billing;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonalReceiptUsageRepository extends JpaRepository<PersonalReceiptUsage, UUID> {}
