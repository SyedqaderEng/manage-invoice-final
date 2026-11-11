package com.manageinvoice.identity;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.manageinvoice.common.security.AuthenticatedUser;
import com.manageinvoice.common.security.TokenRevocationService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Account endpoints for a user already signed in through Supabase Auth. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final TokenRevocationService tokenRevocation;

    public AuthController(AuthService authService, TokenRevocationService tokenRevocation) {
        this.authService = authService;
        this.tokenRevocation = tokenRevocation;
    }

    @PostMapping("/logout")
    public Map<String, Boolean> logout(@AuthenticationPrincipal AuthenticatedUser principal) {
        tokenRevocation.revoke(principal.tokenHash(), principal.tokenExpiresAt());
        return Map.of("loggedOut", true, "tokenRevoked", true);
    }

    @GetMapping("/me")
    public AuthService.UserResponse me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return new AuthService.UserResponse(authService.findRequired(principal.id()));
    }

    @PatchMapping("/me")
    public AuthService.UserResponse updateProfile(@AuthenticationPrincipal AuthenticatedUser principal,
                                                  @Valid @RequestBody ProfileRequest request) {
        return authService.updateProfile(principal.id(), new AuthService.ProfileRequest(request.firstName(), request.lastName()));
    }

    public record ProfileRequest(@NotBlank @Size(max = 100) String firstName, @NotBlank @Size(max = 100) String lastName) {}
}
