package com.sathya.co.biztrack.controller;

import com.sathya.co.biztrack.dto.ProfileRequest;
import com.sathya.co.biztrack.dto.UserResponse;
import com.sathya.co.biztrack.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Profiles")

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    @GetMapping
    public UserResponse get() {
        return profileService.get();
    }

    @PutMapping
    public UserResponse update(@Valid @RequestBody ProfileRequest request) {
        return profileService.update(request);
    }
}