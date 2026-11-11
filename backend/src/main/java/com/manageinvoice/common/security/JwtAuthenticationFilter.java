package com.manageinvoice.common.security;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.manageinvoice.identity.SupabaseAccountService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Authenticates requests with a Supabase Auth access token. The token is verified by Supabase,
 * checked against locally revoked tokens, and the matching application account is provisioned on first use.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final TokenRevocationService tokenRevocation;
    private final SupabaseTokenVerifier verifier;
    private final SupabaseAccountService accounts;

    public JwtAuthenticationFilter(TokenRevocationService tokenRevocation, SupabaseTokenVerifier verifier,
                                   SupabaseAccountService accounts) {
        this.tokenRevocation = tokenRevocation;
        this.verifier = verifier;
        this.accounts = accounts;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            try {
                var identity = verifier.verify(token);
                String tokenHash = TokenRevocationService.hash(token);
                if (identity.isPresent() && !tokenRevocation.isRevoked(tokenHash) && accounts.ensureActive(identity.get())) {
                    var verified = identity.get();
                    authenticate(new AuthenticatedUser(verified.id(), verified.email(), tokenHash, verified.expiresAt()));
                }
            } catch (SupabaseTokenVerifier.VerificationUnavailableException ex) {
                log.warn("Supabase authentication verification unavailable: {}", ex.getMessage());
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Authentication service unavailable");
                return;
            } catch (SupabaseAccountService.AccountConflictException ex) {
                log.warn("Supabase account cannot be linked because an application account already exists");
                response.sendError(HttpServletResponse.SC_CONFLICT, "Account conflict; contact support");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private static void authenticate(AuthenticatedUser principal) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
