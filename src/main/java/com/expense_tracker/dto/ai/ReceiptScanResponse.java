package com.expense_tracker.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptScanResponse {
    private String title;
    private Double amount;
    private String category;
    private String date;
    private String description;
}
