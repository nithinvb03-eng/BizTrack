package com.sathya.co.biztrack.controller;

import com.sathya.co.biztrack.dto.*;
import com.sathya.co.biztrack.service.AuthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.sathya.co.biztrack.service.Access;
import com.sathya.co.biztrack.service.CurrentUser;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Auth")

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return UserResponse.from(authService.register(request));
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpSession session) {
        LoginResponse result = authService.login(request);
        session.setAttribute("userId", result.userId());          // "logged in" = these two values in the session
        session.setAttribute("businessId", result.businessId());
        return result;
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpSession session) {
        session.invalidate();
    }

    @Autowired
    private CurrentUser currentUser;

    @GetMapping("/me")
    public Access me() {
        return currentUser.get();
    }
    
}