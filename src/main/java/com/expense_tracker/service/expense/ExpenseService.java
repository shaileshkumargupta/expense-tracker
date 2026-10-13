package com.expense_tracker.service.expense;

import com.expense_tracker.dto.expense.*;
import jakarta.validation.Valid;

import java.util.List;

public interface ExpenseService {
    void addExpense(ExpenseRequest request, String email);

    List<ExpenseResponse> getExpenses(String email);

    void updateExpense(@Valid UpdateExpenseRequest request, String email);

    ExpenseResponse updateExpenseById(Long id, @Valid ExpenseRequest request, String email);

    void deleteExpense(Long id, String email);

    ExpenseSummaryResponse getSummaryReport(String email);

    int bulkDeleteExpenses(List<Long> expenseIds, String email);

    int bulkCategorizeExpenses(List<Long> expenseIds, String category, String email);

    RecurringExpensesSummaryResponse getRecurringExpenses(String email);

    ExpenseResponse setRecurringStatus(Long id, SetRecurringRequest request, String email);
}
