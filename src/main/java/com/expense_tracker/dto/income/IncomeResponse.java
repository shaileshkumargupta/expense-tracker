package com.expense_tracker.dto.income;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class IncomeResponse {
    private Long id;
    private String source;
    private Double amount;
    private LocalDate dateTime;
    private String description;
}
