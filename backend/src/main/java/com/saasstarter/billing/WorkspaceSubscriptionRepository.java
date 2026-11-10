package com.saasstarter.billing;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceSubscriptionRepository extends JpaRepository<WorkspaceSubscription, UUID> {
    Optional<WorkspaceSubscription> findByWorkspaceId(UUID workspaceId);
    Optional<WorkspaceSubscription> findByStripeSubscriptionId(String stripeSubscriptionId);
}
