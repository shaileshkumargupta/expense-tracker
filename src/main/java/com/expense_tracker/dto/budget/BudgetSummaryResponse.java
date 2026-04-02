package com.expense_tracker.dto.budget;

import lombok.Data;

@Data
public class BudgetSummaryResponse {
    private Double budgetAmount;
    private Double totalExpense;
    private Double remainingAmount;
    private String status;
}
