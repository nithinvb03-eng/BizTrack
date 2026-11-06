package com.sathya.co.biztrack.dto;

import com.sathya.co.biztrack.entity.Role;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterRequest(
        @NotBlank String fullName,
        @NotBlank @Email String email,
        @NotBlank String businessName,
        @NotBlank @Pattern(regexp = Rules.PASSWORD_REGEX, message = Rules.PASSWORD_MESSAGE) String password,
        Role role,
        @AssertTrue(message = "You must accept the terms") boolean termsAccepted) {
}