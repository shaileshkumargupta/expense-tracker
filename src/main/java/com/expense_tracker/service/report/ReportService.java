package com.expense_tracker.service.report;

import com.expense_tracker.dto.report.MonthlyReportResponse;
import com.expense_tracker.entity.expense.Expense;
import com.expense_tracker.entity.income.Income;
import com.expense_tracker.entity.user.User;
import com.expense_tracker.exception.ETMConstantMessages;
import com.expense_tracker.exception.ETMException;
import com.expense_tracker.repository.expense.ExpenseRepository;
import com.expense_tracker.repository.income.IncomeRepository;
import com.expense_tracker.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final UserRepository userRepository;

    public MonthlyReportResponse getMonthlyReport(String email, int month, int year) {
        User user = userRepository.findByEmailId(email)
                .orElseThrow(()-> new ETMException(ETMConstantMessages.USER_NOT_FOUND_CODE,ETMConstantMessages.USER_NOT_FOUND));

        List<Expense> expenses = expenseRepository.findByUserAndMonthAndYear(user,month,year);

        List<Income> incomes = incomeRepository.findByUserAndMonthAndYear(user,month,year);

        double totalExpense = expenses.stream()
                .mapToDouble(Expense::getAmount)
                .sum();

        double totalIncome = incomes.stream()
                .mapToDouble(Income::getAmount)
                .sum();

        Map<String,Double> categoryWise = expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getCategory,
                        Collectors.summingDouble(Expense::getAmount)
                ));


        MonthlyReportResponse response = new MonthlyReportResponse();
        response.setTotalIncome(totalIncome);
        response.setTotalExpense(totalExpense);
        response.setNetSavings(totalIncome-totalExpense);
        response.setCategoryWiseExpense(categoryWise);

        return response;
    }
}
