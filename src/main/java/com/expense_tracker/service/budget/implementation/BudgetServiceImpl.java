package com.expense_tracker.service.budget.implementation;

import com.expense_tracker.dto.budget.BudgetRequest;
import com.expense_tracker.dto.budget.BudgetResponse;
import com.expense_tracker.dto.budget.BudgetSummaryResponse;
import com.expense_tracker.entity.budget.Budget;
import com.expense_tracker.entity.expense.Expense;
import com.expense_tracker.entity.user.User;
import com.expense_tracker.exception.BudgetNotFoundException;
import com.expense_tracker.exception.UserNotFoundException;
import com.expense_tracker.repository.budget.BudgetRepository;
import com.expense_tracker.repository.expense.ExpenseRepository;
import com.expense_tracker.repository.user.UserRepository;
import com.expense_tracker.service.budget.BudgetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class BudgetServiceImpl implements BudgetService {

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    private User getUserByEmail(String email){
        return userRepository.findByEmailId(email)
                .orElseThrow(()-> new UserNotFoundException("User not found"));
    }


    @Override
    public void setBudget(BudgetRequest request, String email) {
        User user = getUserByEmail(email);

        Budget budget = budgetRepository
                .findByUserAndMonthAndYear(user,request.getMonth(),request.getYear())
                .orElse(new Budget());

        budget.setAmount(request.getAmount());
        budget.setMonth(request.getMonth());
        budget.setYear(request.getYear());

        budgetRepository.save(budget);
    }

    @Override
    public BudgetResponse getBudget(Integer month, Integer year, String email) {
        User user = getUserByEmail(email);

        Budget budget = budgetRepository.findByUserAndMonthAndYear(user,month,year)
                .orElseThrow(()-> new BudgetNotFoundException("Budget not found"));

        BudgetResponse res = new BudgetResponse();
        res.setAmount(budget.getAmount());
        res.setMonth(budget.getMonth());
        res.setYear(budget.getYear());
        return res;
    }

    @Override
    public BudgetSummaryResponse getBudgetSummary(Integer month, Integer year, String email) {
        User user = getUserByEmail(email);

        Budget budget = budgetRepository.findByUserAndMonthAndYear(user,month,year)
                .orElseThrow(()-> new BudgetNotFoundException("Budget not found"));

        List<Expense> expenses = expenseRepository.findByUser(user);

        double totalExpense = expenses.stream()
                .filter(e -> e.getDateTime().getMonthValue() == month
                && e.getDateTime().getYear() == year)
                .mapToDouble(Expense::getAmount)
                .sum();

        double remaining = budget.getAmount() - totalExpense;

        BudgetSummaryResponse response = new BudgetSummaryResponse();
        response.setBudgetAmount(budget.getAmount());
        response.setTotalExpense(totalExpense);
        response.setRemainingAmount(remaining);
        response.setStatus(remaining >= 0 ? "SAFE" : "EXCEEDED");

        return response;
    }
}
