package com.saasstarter.billing;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saasstarter.identity.User;
import com.saasstarter.identity.UserRepository;
import com.saasstarter.tenancy.Workspace;
import com.saasstarter.tenancy.WorkspaceRole;
import com.saasstarter.tenancy.WorkspaceService;

@Service
public class BillingService {
    private static final long SIGNATURE_TOLERANCE_SECONDS = 300;
    private static final List<PlanResponse> PLANS = List.of(
        new PlanResponse("starter", "Starter", "The essentials for a small product.", 1900, 19000),
        new PlanResponse("pro", "Pro", "More room for a growing team.", 4900, 49000),
        new PlanResponse("team", "Team", "Advanced capacity for established teams.", 9900, 99000)
    );

    private final WorkspaceSubscriptionRepository subscriptions;
    private final ProcessedStripeEventRepository processedEvents;
    private final WorkspaceService workspaces;
    private final UserRepository users;
    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();
    private final String secretKey;
    private final String webhookSecret;
    private final String successUrl;
    private final String cancelUrl;
    private final String returnUrl;
    private final Map<String, String> priceIds;

    public BillingService(
        WorkspaceSubscriptionRepository subscriptions,
        ProcessedStripeEventRepository processedEvents,
        WorkspaceService workspaces,
        UserRepository users,
        ObjectMapper objectMapper,
        @Value("${billing.stripe.secret-key:}") String secretKey,
        @Value("${billing.stripe.webhook-secret:}") String webhookSecret,
        @Value("${billing.stripe.success-url:http://localhost:5173/app#billing?success=true}") String successUrl,
        @Value("${billing.stripe.cancel-url:http://localhost:5173/app#billing?cancelled=true}") String cancelUrl,
        @Value("${billing.stripe.return-url:http://localhost:5173/app#billing}") String returnUrl,
        @Value("${billing.stripe.prices.starter-monthly:}") String starterMonthly,
        @Value("${billing.stripe.prices.starter-yearly:}") String starterYearly,
        @Value("${billing.stripe.prices.pro-monthly:}") String proMonthly,
        @Value("${billing.stripe.prices.pro-yearly:}") String proYearly,
        @Value("${billing.stripe.prices.team-monthly:}") String teamMonthly,
        @Value("${billing.stripe.prices.team-yearly:}") String teamYearly
    ) {
        this.subscriptions = subscriptions;
        this.processedEvents = processedEvents;
        this.workspaces = workspaces;
        this.users = users;
        this.objectMapper = objectMapper;
        this.secretKey = secretKey;
        this.webhookSecret = webhookSecret;
        this.successUrl = successUrl;
        this.cancelUrl = cancelUrl;
        this.returnUrl = returnUrl;
        this.priceIds = Map.of(
            "starter:monthly", starterMonthly, "starter:yearly", starterYearly,
            "pro:monthly", proMonthly, "pro:yearly", proYearly,
            "team:monthly", teamMonthly, "team:yearly", teamYearly
        );
    }

    public List<PlanResponse> plans() {
        return PLANS;
    }

    public SubscriptionResponse subscription(UUID workspaceId, UUID userId) {
        workspaces.requireMembership(workspaceId, userId);
        return subscriptions.findByWorkspaceId(workspaceId).map(SubscriptionResponse::new).orElse(null);
    }

    public CheckoutResponse createCheckout(UUID workspaceId, UUID userId, CheckoutRequest request) {
        requireBillingAccess(workspaceId, userId);
        String priceId = priceIds.get(request.planId() + ":" + request.interval());
        if (priceId == null || priceId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stripe price is not configured for this plan");
        }
        requireSecretKey();

        workspaces.requireMembership(workspaceId, userId);
        User user = users.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found"));
        WorkspaceSubscription existing = subscriptions.findByWorkspaceId(workspaceId).orElse(null);
        if (existing != null && isActive(existing.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Manage the current subscription through the billing portal");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("mode", "subscription");
        form.add("line_items[0][price]", priceId);
        form.add("line_items[0][quantity]", "1");
        form.add("success_url", successUrl);
        form.add("cancel_url", cancelUrl);
        form.add("client_reference_id", workspaceId.toString());
        form.add("metadata[workspace_id]", workspaceId.toString());
        form.add("metadata[plan_id]", request.planId());
        form.add("metadata[interval]", request.interval());
        form.add("subscription_data[metadata][workspace_id]", workspaceId.toString());
        form.add("subscription_data[metadata][plan_id]", request.planId());
        form.add("subscription_data[metadata][interval]", request.interval());
        form.add("allow_promotion_codes", "true");
        if (existing != null && existing.getStripeCustomerId() != null) {
            form.add("customer", existing.getStripeCustomerId());
        } else {
            form.add("customer_email", user.getEmail());
        }

        JsonNode response = restClient.post()
            .uri("https://api.stripe.com/v1/checkout/sessions")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(JsonNode.class);
        String url = response == null ? null : response.path("url").asText(null);
        if (url == null || url.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Stripe did not return a checkout URL");
        }
        return new CheckoutResponse(url);
    }

    public PortalResponse createPortal(UUID workspaceId, UUID userId) {
        requireBillingAccess(workspaceId, userId);
        requireSecretKey();
        WorkspaceSubscription subscription = subscriptions.findByWorkspaceId(workspaceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No subscription exists for this workspace"));
        if (subscription.getStripeCustomerId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The workspace has no Stripe customer yet");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("customer", subscription.getStripeCustomerId());
        form.add("return_url", returnUrl);
        JsonNode response = restClient.post()
            .uri("https://api.stripe.com/v1/billing_portal/sessions")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(JsonNode.class);
        String url = response == null ? null : response.path("url").asText(null);
        if (url == null || url.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Stripe did not return a billing portal URL");
        }
        return new PortalResponse(url);
    }

    @Transactional
    public void handleWebhook(String payload, String signatureHeader) {
        verifySignature(payload, signatureHeader);
        JsonNode event;
        try {
            event = objectMapper.readTree(payload);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Stripe event payload");
        }
        String eventId = event.path("id").asText("");
        String eventType = event.path("type").asText("");
        if (eventId.isBlank() || eventType.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stripe event id and type are required");
        }
        if (processedEvents.existsById(eventId)) return;

        JsonNode object = event.path("data").path("object");
        switch (eventType) {
            case "checkout.session.completed" -> applyCheckoutCompleted(object);
            case "customer.subscription.created", "customer.subscription.updated" -> applySubscription(object, false);
            case "customer.subscription.deleted" -> applySubscription(object, true);
            default -> { }
        }
        processedEvents.save(new ProcessedStripeEvent(eventId, eventType));
    }

    private void applyCheckoutCompleted(JsonNode object) {
        String workspaceValue = object.path("metadata").path("workspace_id").asText(
            object.path("client_reference_id").asText(""));
        UUID workspaceId = parseWorkspaceId(workspaceValue);
        if (workspaceId == null) return;

        WorkspaceSubscription subscription = subscriptions.findByWorkspaceId(workspaceId).orElse(null);
        if (subscription == null) subscription = newSubscription(workspaceId);
        subscription.setStripeCustomerId(object.path("customer").asText(null));
        subscription.setStripeSubscriptionId(object.path("subscription").asText(null));
        subscription.setPlanId(object.path("metadata").path("plan_id").asText("starter"));
        subscription.setBillingInterval(object.path("metadata").path("interval").asText("monthly"));
        if (!isActive(subscription.getStatus())) subscription.setStatus("pending");
        subscriptions.save(subscription);
    }

    private void applySubscription(JsonNode object, boolean deleted) {
        String stripeSubscriptionId = object.path("id").asText("");
        WorkspaceSubscription subscription = null;
        String workspaceValue = object.path("metadata").path("workspace_id").asText("");
        UUID workspaceId = parseWorkspaceId(workspaceValue);
        if (workspaceId != null) {
            subscription = subscriptions.findByWorkspaceId(workspaceId).orElseGet(() -> newSubscription(workspaceId));
        } else if (!stripeSubscriptionId.isBlank()) {
            subscription = subscriptions.findByStripeSubscriptionId(stripeSubscriptionId).orElse(null);
        }
        if (subscription == null) return;

        subscription.setStripeSubscriptionId(stripeSubscriptionId);
        subscription.setStripeCustomerId(object.path("customer").asText(subscription.getStripeCustomerId()));
        subscription.setPlanId(object.path("metadata").path("plan_id").asText(subscription.getPlanId()));
        subscription.setBillingInterval(object.path("metadata").path("interval").asText(subscription.getBillingInterval()));
        subscription.setStatus(deleted ? "canceled" : object.path("status").asText("unknown"));
        long periodEnd = object.path("current_period_end").asLong(0);
        subscription.setCurrentPeriodEnd(periodEnd > 0 ? Instant.ofEpochSecond(periodEnd) : null);
        subscriptions.save(subscription);
    }

    private WorkspaceSubscription newSubscription(UUID workspaceId) {
        Workspace workspace = workspaces.findWorkspace(workspaceId);
        WorkspaceSubscription subscription = new WorkspaceSubscription();
        subscription.setWorkspace(workspace);
        subscription.setPlanId("starter");
        subscription.setBillingInterval("monthly");
        subscription.setStatus("pending");
        return subscription;
    }

    private void requireBillingAccess(UUID workspaceId, UUID userId) {
        WorkspaceRole role = workspaces.requireRole(workspaceId, userId);
        if (role != WorkspaceRole.OWNER && role != WorkspaceRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Workspace administrator access required");
        }
    }

    private void requireSecretKey() {
        if (secretKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stripe billing is not configured");
        }
    }

    private void verifySignature(String payload, String signatureHeader) {
        if (webhookSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stripe webhook signing is not configured");
        }
        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stripe signature is required");
        }
        String timestamp = null;
        List<String> signatures = new ArrayList<>();
        for (String part : signatureHeader.split(",")) {
            String[] pair = part.trim().split("=", 2);
            if (pair.length != 2) continue;
            if (pair[0].equals("t")) timestamp = pair[1];
            if (pair[0].equals("v1")) signatures.add(pair[1]);
        }
        if (timestamp == null || signatures.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Stripe signature header");
        }
        long signedAt;
        try {
            signedAt = Long.parseLong(timestamp);
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Stripe signature timestamp");
        }
        if (Math.abs(Instant.now().getEpochSecond() - signedAt) > SIGNATURE_TOLERANCE_SECONDS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stripe signature timestamp is outside the allowed window");
        }

        byte[] expected = hmacSha256(timestamp + "." + payload);
        boolean valid = signatures.stream().anyMatch(signature -> {
            try {
                return MessageDigest.isEqual(expected, HexFormat.of().parseHex(signature));
            } catch (IllegalArgumentException exception) {
                return false;
            }
        });
        if (!valid) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Stripe signature");
    }

    private byte[] hmacSha256(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("HMAC-SHA256 is unavailable", exception);
        }
    }

    private UUID parseWorkspaceId(String value) {
        try {
            return value == null || value.isBlank() ? null : UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private boolean isActive(String status) {
        return status.equals("active") || status.equals("trialing") || status.equals("past_due");
    }

    public record PlanResponse(String id, String name, String description, int monthlyPrice, int yearlyPrice) {}
    public record CheckoutRequest(String planId, String interval) {
        public CheckoutRequest {
            if (PLANS.stream().noneMatch(plan -> plan.id().equals(planId))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown plan");
            }
            if (!"monthly".equals(interval) && !"yearly".equals(interval)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Billing interval must be monthly or yearly");
            }
        }
    }
    public record CheckoutResponse(String url) {}
    public record PortalResponse(String url) {}
    public record SubscriptionResponse(String plan, String status, String interval, Instant currentPeriodEnd) {
        SubscriptionResponse(WorkspaceSubscription subscription) {
            this(subscription.getPlanId(), subscription.getStatus(), subscription.getBillingInterval(), subscription.getCurrentPeriodEnd());
        }
    }
}
