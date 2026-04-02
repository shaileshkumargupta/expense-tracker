package com.expense_tracker.dto.budget;

import lombok.Data;

@Data
public class BudgetResponse {
    private Double amount;
    private Integer month;
    private Integer year;
}
