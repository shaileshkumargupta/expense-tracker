package com.expense_tracker.dto.category;

import com.expense_tracker.entity.category.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    private String categoryName;

    @NotNull(message = "Category type is required")
    private CategoryType type;
}
