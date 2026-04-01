package com.expense_tracker.service.expense;

import com.expense_tracker.dto.expense.ExpenseRequest;
import com.expense_tracker.dto.expense.ExpenseResponse;
import com.expense_tracker.dto.expense.ExpenseSummaryResponse;
import com.expense_tracker.dto.expense.UpdateExpenseRequest;
import jakarta.validation.Valid;

import java.util.List;

public interface ExpenseService {
    void addExpense(ExpenseRequest request, String email);

    List<ExpenseResponse> getExpenses(String email);

    void updateExpense(@Valid UpdateExpenseRequest request, String email);

    void deleteExpense(Long id, String email);

    ExpenseSummaryResponse getSummaryReport(String email);
}
