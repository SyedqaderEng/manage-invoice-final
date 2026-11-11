package com.manageinvoice.identity;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Profile operations for the signed-in account. Sign-up and sign-in are handled by Supabase Auth. */
@Service
public class AuthService {
    private final UserRepository users;

    public AuthService(UserRepository users) {
        this.users = users;
    }

    public User findRequired(UUID id) {
        return users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found"));
    }

    @Transactional
    public UserResponse updateProfile(UUID id, ProfileRequest request) {
        User user = findRequired(id);
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        return new UserResponse(user);
    }

    public record ProfileRequest(String firstName, String lastName) {}
    public record UserResponse(UUID id, String email, String firstName, String lastName) {
        public UserResponse(User user) { this(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName()); }
    }
}
