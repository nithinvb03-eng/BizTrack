package com.sathya.co.biztrack.service;

import com.sathya.co.biztrack.dto.ProfileRequest;
import com.sathya.co.biztrack.dto.Rules;
import com.sathya.co.biztrack.dto.UserResponse;
import com.sathya.co.biztrack.entity.User;
import com.sathya.co.biztrack.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProfileService {

    @Autowired private UserRepository users;
    @Autowired private CurrentUser currentUser;
    @Autowired private PasswordEncoder encoder;

    public UserResponse get() {
        return UserResponse.from(load(currentUser.get().userId()));
    }

    public UserResponse update(ProfileRequest r) {
        User u = load(currentUser.get().userId());

        String email = r.email().trim().toLowerCase();
        if (!email.equalsIgnoreCase(u.getEmail()) && users.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That email is already used");
        }
        u.setFullName(r.fullName().trim());
        u.setEmail(email);

        String newPassword = r.newPassword();
        if (newPassword != null && !newPassword.isBlank()) {
            if (r.currentPassword() == null || !encoder.matches(r.currentPassword(), u.getPasswordHash())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
            }
            if (!newPassword.matches(Rules.PASSWORD_REGEX)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Rules.PASSWORD_MESSAGE);
            }
            u.setPasswordHash(encoder.encode(newPassword));
        }
        return UserResponse.from(users.save(u));
    }

    private User load(Integer id) {
        return users.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account no longer exists"));
    }
}