package com.expense_tracker.dto.expense;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkCategorizeRequest {
    @NotEmpty(message = "Expense IDs list cannot be empty")
    private List<Long> expenseIds;

    @NotBlank(message = "Category is required")
    private String category;
}
