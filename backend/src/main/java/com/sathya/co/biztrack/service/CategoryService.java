package com.sathya.co.biztrack.service;

import com.sathya.co.biztrack.entity.Category;
import com.sathya.co.biztrack.entity.TransactionType;
import com.sathya.co.biztrack.repository.CategoryRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {

    @Autowired private CategoryRepository repo;
    @Autowired private CurrentUser currentUser;

    public List<Category> list(TransactionType type) {
        Access a = currentUser.get();
        List<Category> all = repo.findByBusinessIdOrderByCategoryNameAsc(a.businessId());
        if (type == null) {
            return all;
        }
        return all.stream().filter(c -> c.getTransactionType() == type).toList();
    }
}