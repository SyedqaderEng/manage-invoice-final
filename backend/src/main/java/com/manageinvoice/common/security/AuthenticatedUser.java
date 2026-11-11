package com.manageinvoice.common.security;

import java.util.UUID;
import java.time.Instant;

public record AuthenticatedUser(UUID id, String email, String tokenHash, Instant tokenExpiresAt) {
}
