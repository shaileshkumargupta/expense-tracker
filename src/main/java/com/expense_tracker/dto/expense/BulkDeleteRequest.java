package com.expense_tracker.dto.expense;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkDeleteRequest {
    @NotEmpty(message = "Expense IDs list cannot be empty")
    private List<Long> expenseIds;
}
