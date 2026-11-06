package com.sathya.co.biztrack.dto;

import com.sathya.co.biztrack.entity.CreditTakerType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreditTakerRequest(
        @NotBlank @Size(min = 2, max = 100) String name,
        @Pattern(regexp = "^(?:\\+91)?[0-9]{10}$|^$")
        String phone,
        @Email @Size(max = 254) String email,
        CreditTakerType type) {

    public CreditTakerRequest {
        phone = blankToNull(phone);
        email = blankToNull(email);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
