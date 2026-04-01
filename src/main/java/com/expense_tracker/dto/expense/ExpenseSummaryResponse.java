package com.expense_tracker.dto.expense;

import lombok.Data;

import java.util.Map;

@Data
public class ExpenseSummaryResponse {
    private double totalAmount;
    private Integer totalTransaction;
    private Map<String,Double> categorySummary;
}
