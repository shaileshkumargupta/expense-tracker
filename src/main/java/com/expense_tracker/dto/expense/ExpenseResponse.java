package com.expense_tracker.dto.expense;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ExpenseResponse {
    private Long id;
    private String title;
    private Double amount;
    private String category;
    private String dateTime;
    private String description;
    private Boolean isRecurring;
    private String frequency;
    private LocalDate nextDueDate;
}
