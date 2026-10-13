package com.expense_tracker.dto.expense;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecurringExpensesSummaryResponse {
    private List<ExpenseResponse> subscriptions;
    private Double totalMonthlyCommitment;
}
