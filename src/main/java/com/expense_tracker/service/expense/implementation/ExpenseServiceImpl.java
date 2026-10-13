package com.expense_tracker.service.expense.implementation;

import com.expense_tracker.dto.expense.*;
import com.expense_tracker.entity.expense.Expense;
import com.expense_tracker.entity.user.User;
import com.expense_tracker.exception.ETMConstantMessages;
import com.expense_tracker.exception.ETMException;
import com.expense_tracker.repository.expense.ExpenseRepository;
import com.expense_tracker.repository.user.UserRepository;
import com.expense_tracker.service.expense.ExpenseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
    @CacheEvict(value = {"monthlyReports", "dashboardData"}, allEntries = true)
    public void addExpense(ExpenseRequest request, String email) {
        User user = getUserByEmail(email);

        Expense expense = new Expense();
        expense.setTitle(request.getTitle());
        expense.setAmount(request.getAmount());
        expense.setCategory(request.getCategory());
        expense.setDateTime(parseDateTime(request.getDateTime()));
        expense.setDescription(request.getDescription());
        expense.setUser(user);

        expenseRepository.save(expense);
        log.info("Saved expense and evicted monthlyReports and dashboardData caches for user: {}", email);
    }

    @Override
    public List<ExpenseResponse> getExpenses(String email) {
        User user = getUserByEmail(email);
        List<Expense> expenses =  expenseRepository.findByUser(user);

        return expenses.stream().map(this::mapToResponse).toList();
    }

    @Override
    @CacheEvict(value = {"monthlyReports", "dashboardData"}, allEntries = true)
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
        log.info("Updated expense id: {} and evicted caches for user: {}", request.getId(), email);
    }

    @Override
    @CacheEvict(value = {"monthlyReports", "dashboardData"}, allEntries = true)
    public ExpenseResponse updateExpenseById(Long id, ExpenseRequest request, String email) {
        User user = getUserByEmail(email);

        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ETMException(ETMConstantMessages.EXPENSE_NOT_FOUND_CODE, ETMConstantMessages.EXPENSE_NOT_FOUND));

        if (!expense.getUser().getId().equals(user.getId())) {
            log.warn("Unauthorized update attempt for expense id {} by user: {}", id, email);
            throw new ETMException(ETMConstantMessages.NOT_ALLOWED_TO_UPDATE_CODE, ETMConstantMessages.NOT_ALLOWED_TO_UPDATE);
        }

        expense.setTitle(request.getTitle());
        expense.setAmount(request.getAmount());
        expense.setCategory(request.getCategory());
        if (request.getDescription() != null) {
            expense.setDescription(request.getDescription());
        }
        if (request.getDateTime() != null && !request.getDateTime().isBlank()) {
            expense.setDateTime(parseDateTime(request.getDateTime()));
        }

        Expense saved = expenseRepository.save(expense);
        log.info("Updated expense id: {} by path param for user: {}", id, email);
        return mapToResponse(saved);
    }

    @Override
    @CacheEvict(value = {"monthlyReports", "dashboardData"}, allEntries = true)
    public void deleteExpense(Long id, String email) {
        User user = getUserByEmail(email);

        Expense expense = expenseRepository.findById(id)
                .orElseThrow(()->new ETMException(ETMConstantMessages.EXPENSE_NOT_FOUND_CODE,ETMConstantMessages.EXPENSE_NOT_FOUND));

        if (!expense.getUser().getId().equals(user.getId())){
            log.warn("Unauthorized delete attempt by user: {}",email);
            throw new ETMException(ETMConstantMessages.UNAUTHORIZED_CODE,ETMConstantMessages.UNAUTHORIZED);
        }
        expenseRepository.delete(expense);
        log.info("Deleted expense id: {} and evicted caches for user: {}", id, email);
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

        List<ExpenseSummaryResponse.CategorySummary> categoryList = categoryMap.entrySet()
                .stream()
                .map(e-> new ExpenseSummaryResponse.CategorySummary(e.getKey(),e.getValue()))
                .toList();

        ExpenseSummaryResponse response = new ExpenseSummaryResponse();
        response.setTotalAmount(totalAmount);
        response.setTotalTransaction(expenses.size());
        response.setCategorySummary(categoryMap);

        return response;
    }

    @Override
    @Transactional
    @CacheEvict(value = {"monthlyReports", "dashboardData"}, allEntries = true)
    public int bulkDeleteExpenses(List<Long> expenseIds, String email) {
        User user = getUserByEmail(email);

        List<Expense> matchedExpenses = expenseRepository.findByIdInAndUser(expenseIds, user);
        Set<Long> uniqueRequestedIds = new HashSet<>(expenseIds);

        if (matchedExpenses.size() != uniqueRequestedIds.size()) {
            log.warn("Unauthorized or non-existent bulk delete attempt by user: {}. Requested: {}, Matched: {}",
                    email, uniqueRequestedIds.size(), matchedExpenses.size());
            throw new ETMException(ETMConstantMessages.NOT_ALLOWED_TO_UPDATE_CODE,
                    "One or more expenses do not belong to the authenticated user or do not exist");
        }

        int deletedCount = expenseRepository.deleteAllByIdInAndUserId(expenseIds, user.getId());
        log.info("Bulk deleted {} expenses for user: {} and evicted caches", deletedCount, email);
        return deletedCount;
    }

    @Override
    @Transactional
    @CacheEvict(value = {"monthlyReports", "dashboardData"}, allEntries = true)
    public int bulkCategorizeExpenses(List<Long> expenseIds, String category, String email) {
        User user = getUserByEmail(email);

        List<Expense> matchedExpenses = expenseRepository.findByIdInAndUser(expenseIds, user);
        Set<Long> uniqueRequestedIds = new HashSet<>(expenseIds);

        if (matchedExpenses.size() != uniqueRequestedIds.size()) {
            log.warn("Unauthorized or non-existent bulk categorize attempt by user: {}. Requested: {}, Matched: {}",
                    email, uniqueRequestedIds.size(), matchedExpenses.size());
            throw new ETMException(ETMConstantMessages.NOT_ALLOWED_TO_UPDATE_CODE,
                    "One or more expenses do not belong to the authenticated user or do not exist");
        }

        int updatedCount = expenseRepository.updateCategoryForIdsAndUserId(category, expenseIds, user.getId());
        log.info("Bulk categorized {} expenses to '{}' for user: {} and evicted caches", updatedCount, category, email);
        return updatedCount;
    }

    @Override
    public RecurringExpensesSummaryResponse getRecurringExpenses(String email) {
        User user = getUserByEmail(email);
        List<Expense> recurringList = expenseRepository.findByUserAndIsRecurringTrue(user);

        double totalMonthlyCommitment = 0.0;
        for (Expense expense : recurringList) {
            double amt = expense.getAmount() != null ? expense.getAmount() : 0.0;
            String freq = expense.getFrequency() != null ? expense.getFrequency().trim().toUpperCase() : "MONTHLY";
            switch (freq) {
                case "YEARLY" -> totalMonthlyCommitment += (amt / 12.0);
                case "WEEKLY" -> totalMonthlyCommitment += (amt * 4.33);
                default -> totalMonthlyCommitment += amt; // MONTHLY
            }
        }

        totalMonthlyCommitment = Math.round(totalMonthlyCommitment * 100.0) / 100.0;
        List<ExpenseResponse> subscriptions = recurringList.stream().map(this::mapToResponse).toList();

        log.info("Fetched {} recurring subscriptions for user: {}, totalMonthlyCommitment: {}",
                subscriptions.size(), email, totalMonthlyCommitment);
        return new RecurringExpensesSummaryResponse(subscriptions, totalMonthlyCommitment);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"monthlyReports", "dashboardData"}, allEntries = true)
    public ExpenseResponse setRecurringStatus(Long id, SetRecurringRequest request, String email) {
        User user = getUserByEmail(email);

        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ETMException(ETMConstantMessages.EXPENSE_NOT_FOUND_CODE, ETMConstantMessages.EXPENSE_NOT_FOUND));

        if (!expense.getUser().getId().equals(user.getId())) {
            log.warn("Unauthorized attempt to update recurring status for expense id: {} by user: {}", id, email);
            throw new ETMException(ETMConstantMessages.NOT_ALLOWED_TO_UPDATE_CODE, ETMConstantMessages.NOT_ALLOWED_TO_UPDATE);
        }

        expense.setIsRecurring(request.isRecurring());
        if (request.getFrequency() != null && !request.getFrequency().isBlank()) {
            expense.setFrequency(request.getFrequency().trim().toUpperCase());
        }
        if (request.getNextDueDate() != null) {
            expense.setNextDueDate(request.getNextDueDate());
        }

        Expense saved = expenseRepository.save(expense);
        log.info("Updated recurring status for expense id: {} to recurring={} for user: {}", id, request.isRecurring(), email);
        return mapToResponse(saved);
    }

    private ExpenseResponse mapToResponse(Expense expense) {
        ExpenseResponse response = new ExpenseResponse();
        response.setId(expense.getId());
        response.setTitle(expense.getTitle());
        response.setAmount(expense.getAmount());
        response.setCategory(expense.getCategory());
        response.setDateTime(expense.getDateTime() != null ? expense.getDateTime().toString() : null);
        response.setDescription(expense.getDescription());
        response.setIsRecurring(expense.getIsRecurring() != null ? expense.getIsRecurring() : false);
        response.setFrequency(expense.getFrequency());
        response.setNextDueDate(expense.getNextDueDate());
        return response;
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(dateTimeStr);
        } catch (Exception ignored) {
            try {
                LocalDate localDate = LocalDate.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE);
                return localDate.atStartOfDay();
            } catch (Exception e) {
                log.warn("Could not parse dateTime string '{}', defaulting to now()", dateTimeStr);
                return LocalDateTime.now();
            }
        }
    }
}
