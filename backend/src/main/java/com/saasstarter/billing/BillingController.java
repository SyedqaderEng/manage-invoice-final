package com.saasstarter.billing;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.saasstarter.common.security.AuthenticatedUser;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {
    private final BillingService billing;

    public BillingController(BillingService billing) {
        this.billing = billing;
    }

    @GetMapping("/plans")
    public List<BillingService.PlanResponse> plans() {
        return billing.plans();
    }

    @GetMapping("/workspaces/{workspaceId}")
    public ResponseEntity<BillingService.SubscriptionResponse> subscription(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID workspaceId
    ) {
        BillingService.SubscriptionResponse subscription = billing.subscription(workspaceId, principal.id());
        return subscription == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(subscription);
    }

    @PostMapping("/workspaces/{workspaceId}/checkout")
    public BillingService.CheckoutResponse checkout(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID workspaceId,
        @Valid @RequestBody CheckoutRequest request
    ) {
        return billing.createCheckout(workspaceId, principal.id(), new BillingService.CheckoutRequest(request.planId(), request.interval()));
    }

    @PostMapping("/workspaces/{workspaceId}/portal")
    public BillingService.PortalResponse portal(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID workspaceId
    ) {
        return billing.createPortal(workspaceId, principal.id());
    }

    @PostMapping(value = "/webhook", consumes = "application/json")
    public ResponseEntity<Void> webhook(
        @RequestBody String payload,
        @RequestHeader(value = "Stripe-Signature", required = false) String signature
    ) {
        billing.handleWebhook(payload, signature);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    public record CheckoutRequest(@NotBlank String planId, @NotBlank String interval) {}
}
