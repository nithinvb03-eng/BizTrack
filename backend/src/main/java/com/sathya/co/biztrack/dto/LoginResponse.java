package com.sathya.co.biztrack.dto;

import com.sathya.co.biztrack.entity.Role;

public record LoginResponse(Integer userId, String fullName, String email,
                            Integer businessId, Role role) {
}