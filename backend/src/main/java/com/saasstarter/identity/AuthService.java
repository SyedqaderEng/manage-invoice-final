package com.saasstarter.identity;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.saasstarter.common.security.JwtService;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        users.save(user);
        return createResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmailIgnoreCase(request.email().trim().toLowerCase())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash()) || user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        return createResponse(user);
    }

    public User findRequired(java.util.UUID id) {
        return users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found"));
    }

    @Transactional
    public UserResponse updateProfile(java.util.UUID id, ProfileRequest request) {
        User user = findRequired(id);
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        return new UserResponse(user);
    }

    private AuthResponse createResponse(User user) {
        return new AuthResponse(jwtService.createToken(user.getId(), user.getEmail()), new UserResponse(user));
    }

    public record RegisterRequest(String email, String password, String firstName, String lastName) {}
    public record LoginRequest(String email, String password) {}
    public record ProfileRequest(String firstName, String lastName) {}
    public record AuthResponse(String accessToken, UserResponse user) {}
    public record UserResponse(java.util.UUID id, String email, String firstName, String lastName) {
        public UserResponse(User user) { this(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName()); }
    }
}
