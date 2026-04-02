package com.expense_tracker.service.budget;

import com.expense_tracker.dto.budget.BudgetRequest;
import com.expense_tracker.dto.budget.BudgetResponse;
import com.expense_tracker.dto.budget.BudgetSummaryResponse;
import jakarta.validation.Valid;

public interface BudgetService {
    void setBudget(@Valid BudgetRequest request, String email);

    BudgetResponse getBudget(Integer month, Integer year, String email);

    BudgetSummaryResponse getBudgetSummary(Integer month, Integer year, String email);
}
