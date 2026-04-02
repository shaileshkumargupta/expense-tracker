package com.expense_tracker.service.income;

import com.expense_tracker.dto.expense.UpdateExpenseRequest;
import com.expense_tracker.dto.income.IncomeRequest;
import com.expense_tracker.dto.income.IncomeResponse;
import com.expense_tracker.dto.income.IncomeSummaryResponse;
import com.expense_tracker.dto.income.UpdateIncomeRequest;
import jakarta.validation.Valid;

import java.util.List;

public interface IncomeService {
    void addIncome(@Valid IncomeRequest request, String email);

    List<IncomeResponse> getIncomes(String email);

    void updateIncome(@Valid UpdateIncomeRequest request, String email);

    void deleteIncome(Long id, String email);

    IncomeSummaryResponse getIncomeSummaryReport(String email);
}
