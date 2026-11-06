package com.sathya.co.biztrack.dto;

import com.sathya.co.biztrack.entity.Role;

public record MemberResponse(Integer memberId, Integer userId, String fullName, String email, Role role) {
}