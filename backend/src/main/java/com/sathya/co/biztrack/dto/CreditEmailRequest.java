package com.sathya.co.biztrack.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record CreditEmailRequest(@Email @Size(max = 254) String toEmail) {

    public CreditEmailRequest {
        toEmail = toEmail == null || toEmail.isBlank() ? null : toEmail.trim();
    }
}
