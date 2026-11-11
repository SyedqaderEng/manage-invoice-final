package com.manageinvoice.billing;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersonalUsageService {
    private final PersonalReceiptUsageRepository usage;

    public PersonalUsageService(PersonalReceiptUsageRepository usage) {
        this.usage = usage;
    }

    @Transactional(readOnly = true)
    public UsageResponse get(UUID userId) {
        return usage.findById(userId)
            .map(value -> new UsageResponse(value.getTrackedUploads(), value.isHistoryComplete(), false))
            .orElseGet(() -> new UsageResponse(0, true, false));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordUpload(UUID userId) {
        PersonalReceiptUsage value = usage.findById(userId).orElseGet(() -> new PersonalReceiptUsage(userId));
        value.recordUpload();
        usage.save(value);
    }

    public record UsageResponse(long trackedUploads, boolean historyComplete, boolean limitEnforced) {}
}
