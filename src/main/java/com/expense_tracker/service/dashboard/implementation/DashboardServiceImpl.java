package com.expense_tracker.service.dashboard.implementation;

import com.expense_tracker.dto.dashboard.DashboardResponse;
import com.expense_tracker.dto.user.UserSummary;
import com.expense_tracker.entity.budget.Budget;
import com.expense_tracker.entity.expense.Expense;
import com.expense_tracker.entity.income.Income;
import com.expense_tracker.entity.user.User;
import com.expense_tracker.exception.UserNotFoundException;
import com.expense_tracker.repository.budget.BudgetRepository;
import com.expense_tracker.repository.expense.ExpenseRepository;
import com.expense_tracker.repository.income.IncomeRepository;
import com.expense_tracker.repository.user.UserRepository;
import com.expense_tracker.service.dashboard.DashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class DashboardServiceImpl implements DashboardService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private IncomeRepository incomeRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    private User getUserByEmail(String email){
        return userRepository.findByEmailId(email)
                .orElseThrow(()-> new UserNotFoundException("User not found"));
    }

    @Override
    public DashboardResponse getDashBoard(Integer month, Integer year, String email) {
        User user = getUserByEmail(email);

        List<Expense> expenses = expenseRepository.findByUser(user);
        List<Income> incomes = incomeRepository.findByUser(user);

        double totalExpense = 0;
        double totalIncome = 0;

        Map<String,Double> expenseMap = new HashMap<>();
        Map<String,Double> incomeMap = new HashMap<>();

        for (Expense expense : expenses){
            if (expense.getDateTime().getMonthValue() == month && expense.getDateTime().getYear() == year){
                totalExpense += expense.getAmount();
                expenseMap.put(expense.getCategory(),expenseMap.getOrDefault(expense.getCategory(),0.0)+expense.getAmount());
            }
        }

        for (Income income : incomes){
            if (income.getDateTime().getMonthValue() == month && income.getDateTime().getYear() == year){
                totalIncome += income.getAmount();
                incomeMap.put(income.getSource(),incomeMap.getOrDefault(income.getSource(),0.0)+income.getAmount());
            }
        }

        double balance = totalIncome - totalExpense;

        Budget budget = budgetRepository.findByUserAndMonthAndYear(user,month,year).orElse(null);

        double budgetAmount = budget != null ? budget.getAmount() : 0;
        double remaining = budgetAmount - totalExpense;

        UserSummary userSummary = new UserSummary();
        userSummary.setUserId(user.getId());
        userSummary.setName(user.getName());
        userSummary.setEmail(user.getEmailId());
        userSummary.setMobileNumber(user.getMobileNumber());

        DashboardResponse res = new DashboardResponse();
        res.setTotalIncome(totalIncome);
        res.setTotalExpense(totalExpense);
        res.setBalance(balance);
        res.setBudget(budgetAmount);
        res.setRemainingBudget(remaining);
        res.setExpenseCategorySummary(expenseMap);
        res.setIncomeSourceSummary(incomeMap);
        res.setUser(userSummary);

        return res;
    }
}
