package com.expense_tracker.dto.goal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoalContributionRequest {

    @NotNull(message = "Contribution amount is required")
    @Positive(message = "Contribution amount must be greater than 0")
    private Double amount;
}
