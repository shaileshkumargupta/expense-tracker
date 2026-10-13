package com.expense_tracker.dto.goal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalResponse {
    private Long id;
    private String name;
    private Double targetAmount;
    private Double currentAmount;
    private Double percentageCompleted;
    private LocalDate targetDate;
    private String category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
