package com.expense_tracker.dto.report;

import lombok.Data;
import java.util.Map;

@Data
public class MonthlyReportResponse {
    private Double totalIncome;
    private Double totalExpense;
    private Double netSavings;

    private Map<String, Double> categoryWiseExpense;
}
