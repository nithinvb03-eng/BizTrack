package com.sathya.co.biztrack.repository;

import com.sathya.co.biztrack.entity.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    List<Category> findByBusinessIdOrderByCategoryNameAsc(Integer businessId);
    Optional<Category> findByIdAndBusinessId(Integer id, Integer businessId);
}