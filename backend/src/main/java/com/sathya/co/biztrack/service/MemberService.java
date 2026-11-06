package com.sathya.co.biztrack.service;

import com.sathya.co.biztrack.dto.*;
import com.sathya.co.biztrack.entity.*;
import com.sathya.co.biztrack.repository.*;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MemberService {

    @Autowired private BusinessMemberRepository members;
    @Autowired private UserRepository users;
    @Autowired private CurrentUser currentUser;
    @Autowired private AuditService audit;

    public List<MemberResponse> list() {
        Access a = currentUser.mustBe(Role::canManageMembers);        // owner only
        List<MemberResponse> result = new ArrayList<>();
        for (BusinessMember m : members.findByBusinessId(a.businessId())) {
            User u = users.findById(m.getUserId()).orElseThrow();
            result.add(toResponse(m, u));
        }
        return result;
    }

    /** Add by email: the person must already have registered. */
    @Transactional
    public MemberResponse add(AddMemberRequest r) {
        Access a = currentUser.mustBe(Role::canManageMembers);
        if (r.role() == Role.OWNER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose EMPLOYEE or VIEWER");
        }
        User u = users.findByEmailIgnoreCase(r.email().trim()).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "No account with this email. Ask them to register first."));
        if (members.findByBusinessIdAndUserId(a.businessId(), u.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This person is already a member");
        }
        BusinessMember m = new BusinessMember();
        m.setBusinessId(a.businessId());
        m.setUserId(u.getId());
        m.setRole(r.role());
        members.save(m);
        audit.log(a.businessId(), a.userId(), "MEMBER_ADD", "MEMBER", m.getId().longValue());
        return toResponse(m, u);
    }

    @Transactional
    public MemberResponse changeRole(Integer memberId, Role newRole) {
        Access a = currentUser.mustBe(Role::canManageMembers);
        if (newRole == Role.OWNER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose EMPLOYEE or VIEWER");
        }
        BusinessMember m = members.findByIdAndBusinessId(memberId, a.businessId()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
        if (m.getRole() == Role.OWNER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The owner's role cannot be changed");
        }
        m.setRole(newRole);
        members.save(m);
        audit.log(a.businessId(), a.userId(), "ROLE_CHANGE", "MEMBER", m.getId().longValue());
        return toResponse(m, users.findById(m.getUserId()).orElseThrow());
    }

    private MemberResponse toResponse(BusinessMember m, User u) {
        return new MemberResponse(m.getId(), u.getId(), u.getFullName(), u.getEmail(), m.getRole());
    }
}