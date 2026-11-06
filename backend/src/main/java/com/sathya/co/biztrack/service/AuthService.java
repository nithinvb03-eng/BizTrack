package com.sathya.co.biztrack.service;

import com.sathya.co.biztrack.dto.*;
import com.sathya.co.biztrack.entity.*;
import com.sathya.co.biztrack.repository.*;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    @Autowired private UserRepository users;
    @Autowired private BusinessRepository businesses;
    @Autowired private BusinessMemberRepository members;
    @Autowired private CategoryRepository categories;
    @Autowired private PasswordEncoder encoder;

    @Transactional
    public User register(RegisterRequest r) {
        if (users.existsByEmailIgnoreCase(r.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        User user = new User();
        user.setFullName(r.fullName().trim());
        user.setEmail(r.email().trim().toLowerCase());
        user.setPasswordHash(encoder.encode(r.password()));
        users.save(user);

        Business business = new Business();
        business.setBusinessName(r.businessName().trim());
        business.setOwnerUserId(user.getId());
        businesses.save(business);

        BusinessMember member = new BusinessMember();
        member.setBusinessId(business.getId());
        member.setUserId(user.getId());
        member.setRole(r.role() == null ? Role.OWNER : r.role());
        members.save(member);

        for (String name : List.of("Sales", "Service", "Other income")) {
            addCategory(business.getId(), name, TransactionType.INCOME);
        }
        for (String name : List.of("Utilities", "Supplies", "Rent", "Salary")) {
            addCategory(business.getId(), name, TransactionType.EXPENSE);
        }
        return user;
    }

    public LoginResponse login(LoginRequest r) {
        User u = users.findByEmailIgnoreCase(r.email().trim()).orElseThrow(this::invalid);
        if (!encoder.matches(r.password(), u.getPasswordHash())) {
            throw invalid();
        }
        if (!"ACTIVE".equals(u.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active");
        }
        List<BusinessMember> mine = members.findByUserId(u.getId());
        if (mine.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No business found for this account");
        }
        BusinessMember m = mine.get(0);      // first business; switching businesses can come later
        return new LoginResponse(u.getId(), u.getFullName(), u.getEmail(), m.getBusinessId(), m.getRole());
    }

    private void addCategory(Integer businessId, String name, TransactionType type) {
        Category c = new Category();
        c.setBusinessId(businessId);
        c.setCategoryName(name);
        c.setTransactionType(type);
        categories.save(c);
    }

    private ResponseStatusException invalid() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }
}