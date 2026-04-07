package com.expense_tracker.service.category;

import com.expense_tracker.dto.category.CategoryRequest;
import com.expense_tracker.dto.category.CategoryResponse;
import com.expense_tracker.entity.category.CategoryType;
import jakarta.validation.Valid;

import java.util.List;

public interface CategoryService {
    void createCategory(@Valid CategoryRequest request, String email);

    List<CategoryResponse> getCategories(CategoryType type, String email);
}
