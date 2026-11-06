package com.sathya.co.biztrack.controller;

import com.sathya.co.biztrack.dto.AddMemberRequest;
import com.sathya.co.biztrack.dto.MemberResponse;
import com.sathya.co.biztrack.dto.RoleRequest;
import com.sathya.co.biztrack.service.MemberService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Members")

@RestController
@RequestMapping("/api/members")
public class MemberController {

    @Autowired
    private MemberService memberService;

    @GetMapping
    public List<MemberResponse> list() {
        return memberService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse add(@Valid @RequestBody AddMemberRequest request) {
        return memberService.add(request);
    }

    @PatchMapping("/{id}/role")
    public MemberResponse changeRole(@PathVariable Integer id, @Valid @RequestBody RoleRequest request) {
        return memberService.changeRole(id, request.role());
    }
}