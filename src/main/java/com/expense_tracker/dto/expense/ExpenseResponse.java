package com.expense_tracker.dto.expense;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExpenseResponse {
    private Long id;
    private String title;
    private Double amount;
    private String category;
    private String dateTime;
    private String description;
}
