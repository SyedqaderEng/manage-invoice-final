package com.manageinvoice.billing;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.manageinvoice.common.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/v1/billing/personal")
public class PersonalPlanController {
    private final PersonalUsageService usage;

    public PersonalPlanController(PersonalUsageService usage) {
        this.usage = usage;
    }

    @GetMapping("/plans")
    public List<PersonalPlan> plans() {
        return List.of(
            new PersonalPlan("free", "Free", 0, 0, "usd", 30, false, 0, false),
            new PersonalPlan("pro", "Pro", 1000, 6700, "usd", null, true, 1, false)
        );
    }

    @GetMapping("/usage")
    public PersonalUsageService.UsageResponse usage(@AuthenticationPrincipal AuthenticatedUser principal) {
        return usage.get(principal.id());
    }

    public record PersonalPlan(String id, String name, int monthlyCents, int yearlyCents,
                               String currency, Integer lifetimeFreeReceipts, boolean unlimitedReceipts, int trialMonths,
                               boolean checkoutAvailable) {}
}
