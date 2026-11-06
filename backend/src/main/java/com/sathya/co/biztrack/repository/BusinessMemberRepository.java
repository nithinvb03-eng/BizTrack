package com.sathya.co.biztrack.repository;

import com.sathya.co.biztrack.entity.BusinessMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessMemberRepository extends JpaRepository<BusinessMember, Integer> {
    List<BusinessMember> findByBusinessId(Integer businessId);
    List<BusinessMember> findByUserId(Integer userId);
    Optional<BusinessMember> findByBusinessIdAndUserId(Integer businessId, Integer userId);
    Optional<BusinessMember> findByIdAndBusinessId(Integer id, Integer businessId);
}