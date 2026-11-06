package com.sathya.co.biztrack.dto;

import com.sathya.co.biztrack.entity.Role;
import jakarta.validation.constraints.NotNull;

public record RoleRequest(@NotNull Role role) {
}