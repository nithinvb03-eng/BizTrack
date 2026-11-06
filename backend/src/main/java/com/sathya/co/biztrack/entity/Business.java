package com.sathya.co.biztrack.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "businesses")
@Getter
@Setter
public class Business extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "business_id")
    private Integer id;

    @Column(nullable = false)
    private String businessName;

    @Column(nullable = false)
    private Integer ownerUserId;

    @Column(length = 20)
    private String gstin;
}