package com.expense_tracker.dto.expense;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SetRecurringRequest {
    private boolean recurring;
    private String frequency; // MONTHLY, YEARLY, WEEKLY
    private LocalDate nextDueDate;
}
