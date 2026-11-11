package com.manageinvoice.common.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenRevocationService {
    private final RevokedTokenRepository revokedTokens;

    public TokenRevocationService(RevokedTokenRepository revokedTokens) {
        this.revokedTokens = revokedTokens;
    }

    public boolean isRevoked(String tokenHash) {
        return revokedTokens.existsById(tokenHash);
    }

    @Transactional
    public void revoke(String tokenHash, Instant expiresAt) {
        revokedTokens.deleteByExpiresAtBefore(Instant.now());
        revokedTokens.save(new RevokedToken(tokenHash, expiresAt));
    }

    public static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
