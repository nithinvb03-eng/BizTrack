package com.sathya.co.biztrack.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ProfileRequest(@NotBlank String fullName, @NotBlank @Email String email,
                             String currentPassword, String newPassword) {
}