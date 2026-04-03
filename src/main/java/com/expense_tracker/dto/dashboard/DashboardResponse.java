package com.expense_tracker.dto.dashboard;

import com.expense_tracker.dto.user.UserSummary;
import lombok.Data;

import java.util.Map;

@Data
public class DashboardResponse {

    private UserSummary user;

    private Double totalIncome;
    private Double totalExpense;
    private Double balance;

    private Double budget;
    private Double remainingBudget;

    private Map<String, Double> expenseCategorySummary;
    private Map<String, Double> incomeSourceSummary;
}
