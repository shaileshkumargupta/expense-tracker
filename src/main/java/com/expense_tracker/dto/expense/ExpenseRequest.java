package com.expense_tracker.dto.expense;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ExpenseRequest {
    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than 0")
    private Double amount;

    @NotBlank(message = "Category is required")
    private String category;

    @Size(max = 255, message = "Description too long")
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd[ HH:mm:ss][.SSSSSS]")
    private String dateTime;
}
