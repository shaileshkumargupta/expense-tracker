package com.expense_tracker.dto.recurringTxn;

import com.expense_tracker.entity.category.CategoryType;
import com.expense_tracker.entity.recurringTxn.Frequency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class RecurringRequest {

    @NotBlank
    private String title;

    @NotNull
    @Positive
    private Double amount;

    @NotNull
    private Frequency frequency;

    @NotNull
    private CategoryType type;

    @NotNull
    private Long categoryId;
}
