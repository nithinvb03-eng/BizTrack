package com.sathya.co.biztrack.controller;

import com.sathya.co.biztrack.entity.Category;
import com.sathya.co.biztrack.entity.TransactionType;
import com.sathya.co.biztrack.service.CategoryService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Categories")

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping
    public List<Category> list(@RequestParam(required = false) TransactionType type) {
        return categoryService.list(type);
    }
}