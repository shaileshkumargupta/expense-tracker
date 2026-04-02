package com.expense_tracker.dto.income;

import lombok.Data;

import java.util.Map;

@Data
public class IncomeSummaryResponse {
    private double totalAmount;
    private Integer totalTransaction;
    private Map<String,Double> categorySummary;
}
