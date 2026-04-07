package com.expense_tracker.dto.category;

import com.expense_tracker.entity.category.CategoryType;
import lombok.Data;

@Data
public class CategoryResponse {
    private Long categoryId;
    private String categoryName;
    private CategoryType type;
}
