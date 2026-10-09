package com.expense_tracker.dto.expense;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
public class ExpenseSummaryResponse {
    private double totalAmount;
    private Integer totalTransaction;
    private Map<String,Double> categorySummary;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategorySummary {
        private String category;
        private Double amount;
    }
}
