package com.sathya.co.biztrack.service;

import com.sathya.co.biztrack.entity.Role;

public record Access(Integer userId, Integer businessId, Role role) {
}