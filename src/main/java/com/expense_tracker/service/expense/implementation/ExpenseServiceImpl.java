package com.expense_tracker.service.expense.implementation;

import com.expense_tracker.dto.expense.ExpenseRequest;
import com.expense_tracker.dto.expense.ExpenseResponse;
import com.expense_tracker.dto.expense.ExpenseSummaryResponse;
import com.expense_tracker.dto.expense.UpdateExpenseRequest;
import com.expense_tracker.entity.expense.Expense;
import com.expense_tracker.entity.user.User;
import com.expense_tracker.exception.ETMConstantMessages;
import com.expense_tracker.exception.ETMException;
import com.expense_tracker.repository.expense.ExpenseRepository;
import com.expense_tracker.repository.user.UserRepository;
import com.expense_tracker.service.expense.ExpenseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ExpenseServiceImpl implements ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private UserRepository userRepository;

    private User getUserByEmail(String email){
        return userRepository.findByEmailId(email)
                .orElseThrow(()-> new ETMException(ETMConstantMessages.USER_NOT_FOUND_CODE,ETMConstantMessages.USER_NOT_FOUND));
    }

    @Override
    public void addExpense(ExpenseRequest request, String email) {
        User user = getUserByEmail(email);

        Expense expense = new Expense();
        expense.setTitle(request.getTitle());
        expense.setAmount(request.getAmount());
        expense.setCategory(request.getCategory());
        expense.setDateTime(LocalDateTime.now());
        expense.setDescription(request.getDescription());
        expense.setUser(user);

        expenseRepository.save(expense);
    }

    @Override
    public List<ExpenseResponse> getExpenses(String email) {
        User user = getUserByEmail(email);
        List<Expense> expenses =  expenseRepository.findByUser(user);

        return expenses.stream().map(expense -> {
            ExpenseResponse response = new ExpenseResponse();
            response.setId(expense.getId());
            response.setTitle(expense.getTitle());
            response.setAmount(expense.getAmount());
            response.setCategory(expense.getCategory());
            response.setDateTime(expense.getDateTime());
            response.setDescription(expense.getDescription());
            return response;
        }).toList();
    }

    @Override
    public void updateExpense(UpdateExpenseRequest request, String email) {
        User user = getUserByEmail(email);

        Expense expense = expenseRepository.findById(request.getId())
                .orElseThrow(()->new ETMException(ETMConstantMessages.EXPENSE_NOT_FOUND_CODE,ETMConstantMessages.EXPENSE_NOT_FOUND));

        if (!expense.getUser().getId().equals(user.getId())){
            log.warn("Unauthorized update attempt by user: {}",email);
            throw new ETMException(ETMConstantMessages.NOT_ALLOWED_TO_UPDATE_CODE,ETMConstantMessages.NOT_ALLOWED_TO_UPDATE);
        }

        expense.setTitle(request.getTitle());
        expense.setAmount(request.getAmount());
        expense.setCategory(request.getCategory());
        expense.setDescription(request.getDescription());
        expenseRepository.save(expense);
    }

    @Override
    public void deleteExpense(Long id, String email) {
        User user = getUserByEmail(email);

        Expense expense = expenseRepository.findById(id)
                .orElseThrow(()->new ETMException(ETMConstantMessages.EXPENSE_NOT_FOUND_CODE,ETMConstantMessages.EXPENSE_NOT_FOUND));

        if (!expense.getUser().getId().equals(user.getId())){
            log.warn("Unauthorized delete attempt by user: {}",email);
            throw new ETMException(ETMConstantMessages.UNAUTHORIZED_CODE,ETMConstantMessages.UNAUTHORIZED);
        }
        expenseRepository.delete(expense);
    }

    @Override
    public ExpenseSummaryResponse getSummaryReport(String email) {
        User user = getUserByEmail(email);

        List<Expense> expenses = expenseRepository.findByUser(user);

        double totalAmount = 0;
        Map<String,Double> categoryMap = new HashMap<>();

        for (Expense expense : expenses){
            totalAmount += expense.getAmount();
            categoryMap.put(expense.getCategory(),
                    categoryMap.getOrDefault(expense.getCategory(),0.0)+expense.getAmount());
        }
        ExpenseSummaryResponse response = new ExpenseSummaryResponse();
        response.setTotalAmount(totalAmount);
        response.setTotalTransaction(expenses.size());
        response.setCategorySummary(categoryMap);

        return response;
    }
}
