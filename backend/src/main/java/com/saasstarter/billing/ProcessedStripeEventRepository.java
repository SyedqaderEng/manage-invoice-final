package com.saasstarter.billing;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedStripeEventRepository extends JpaRepository<ProcessedStripeEvent, String> {
}
