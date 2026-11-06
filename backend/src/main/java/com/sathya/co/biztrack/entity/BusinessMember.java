package com.sathya.co.biztrack.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "business_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"business_id", "user_id"}))
@Getter
@Setter
public class BusinessMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Integer id;

    @Column(nullable = false)
    private Integer businessId;

    @Column(nullable = false)
    private Integer userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
}