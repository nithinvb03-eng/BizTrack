package com.sathya.co.biztrack.service;

import com.sathya.co.biztrack.entity.BusinessMember;
import com.sathya.co.biztrack.entity.Role;
import com.sathya.co.biztrack.repository.BusinessMemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.function.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CurrentUser {

    @Autowired private HttpServletRequest request;
    @Autowired private BusinessMemberRepository members;

    /** Must be logged in, otherwise 401. */
    public Access get() {
        HttpSession session = request.getSession(false);
        Integer userId = (session == null) ? null : (Integer) session.getAttribute("userId");
        Integer businessId = (session == null) ? null : (Integer) session.getAttribute("businessId");
        if (userId == null || businessId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in first");
        }
        BusinessMember m = members.findByBusinessIdAndUserId(businessId, userId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this business"));
        return new Access(userId, businessId, m.getRole());
    }

    /** Must be logged in AND the role must allow it, otherwise 403. Example: mustBe(Role::canAdd) */
    public Access mustBe(Predicate<Role> allowed) {
        Access access = get();
        if (!allowed.test(access.role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Your role (" + access.role() + ") is not allowed to do this");
        }
        return access;
    }
}