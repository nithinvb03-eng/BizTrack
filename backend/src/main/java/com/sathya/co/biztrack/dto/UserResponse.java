package com.sathya.co.biztrack.dto;

import com.sathya.co.biztrack.entity.User;

public record UserResponse(Integer id, String fullName, String email) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getFullName(), u.getEmail());
    }
}