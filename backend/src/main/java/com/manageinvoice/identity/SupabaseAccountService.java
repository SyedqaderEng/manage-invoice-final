package com.manageinvoice.identity;

import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.manageinvoice.common.security.SupabaseTokenVerifier.Identity;

@Service
public class SupabaseAccountService {
    private final UserRepository users;
    private final TransactionTemplate transactions;

    public SupabaseAccountService(UserRepository users, TransactionTemplate transactions) {
        this.users = users;
        this.transactions = transactions;
    }

    public boolean ensureActive(Identity identity) {
        User user = users.findById(identity.id()).orElse(null);
        if (user == null) {
            try {
                user = transactions.execute(status -> {
                    User created = new User();
                    created.setId(identity.id());
                    created.setEmail(identity.email().trim().toLowerCase(Locale.ROOT));
                    created.setFirstName(identity.firstName());
                    created.setLastName(identity.lastName());
                    created.setDisplayName(identity.displayName().isBlank() ? null : identity.displayName());
                    return users.saveAndFlush(created);
                });
            } catch (DataIntegrityViolationException ex) {
                user = users.findById(identity.id()).orElseThrow(() ->
                    new AccountConflictException("Supabase account conflicts with an existing application account", ex));
            }
        }
        if (user == null) {
            throw new IllegalStateException("Account creation returned no user");
        }
        if (user.getDisplayName() == null && !identity.displayName().isBlank()) {
            user = transactions.execute(status -> {
                User existing = users.findByIdForUpdate(identity.id()).orElseThrow();
                if (existing.getDisplayName() == null) existing.setDisplayName(identity.displayName());
                return existing;
            });
        }
        return user.getStatus() == UserStatus.ACTIVE;
    }

    public static class AccountConflictException extends RuntimeException {
        AccountConflictException(String message, Throwable cause) { super(message, cause); }
    }
}
