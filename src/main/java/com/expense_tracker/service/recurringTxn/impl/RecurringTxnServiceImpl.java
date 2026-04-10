package com.expense_tracker.service.recurringTxn.impl;

import com.expense_tracker.dto.recurringTxn.RecurringRequest;
import com.expense_tracker.entity.category.Category;
import com.expense_tracker.entity.category.CategoryType;
import com.expense_tracker.entity.expense.Expense;
import com.expense_tracker.entity.income.Income;
import com.expense_tracker.entity.recurringTxn.RecurringTransaction;
import com.expense_tracker.entity.user.User;
import com.expense_tracker.exception.ETMConstantMessages;
import com.expense_tracker.exception.ETMException;
import com.expense_tracker.repository.category.CategoryRepository;
import com.expense_tracker.repository.expense.ExpenseRepository;
import com.expense_tracker.repository.income.IncomeRepository;
import com.expense_tracker.repository.recurringTxn.RecurringRepository;
import com.expense_tracker.repository.user.UserRepository;
import com.expense_tracker.service.recurringTxn.RecurringTxnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringTxnServiceImpl implements RecurringTxnService {

    private final UserRepository userRepository;

    private final CategoryRepository categoryRepository;

    private final RecurringRepository recurringRepository;

    private final ExpenseRepository expenseRepository;

    private final IncomeRepository incomeRepository;

    private User getUserEmail(String email){
        return userRepository.findByEmailId(email)
                .orElseThrow(()-> new ETMException(ETMConstantMessages.USER_NOT_FOUND_CODE,ETMConstantMessages.USER_NOT_FOUND));
    }

    @Override
    public void createRecurring(RecurringRequest request, String email) {
        User user = getUserEmail(email);

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(()-> new ETMException(ETMConstantMessages.CATEGORY_NOT_FOUND_CODE,ETMConstantMessages.CATEGORY_NOT_FOUND));

        RecurringTransaction rt = new RecurringTransaction();
        rt.setTitle(request.getTitle());
        rt.setAmount(request.getAmount());
        rt.setFrequency(request.getFrequency());
        rt.setType(request.getType());
        rt.setUser(user);
        rt.setCategory(category);

        rt.setNextExecutionTime(LocalDateTime.now());

        recurringRepository.save(rt);
    }

    public boolean processRecurringTxn(){
        List<RecurringTransaction> dueList = recurringRepository.findByNextExecutionTimeBefore(LocalDateTime.now());

        if (dueList.isEmpty()){
            return false;
        }
        for (RecurringTransaction rt : dueList){
            try {
                if (rt.getType() == CategoryType.EXPENSE) {
                    Expense expense = new Expense();
                    expense.setTitle(rt.getTitle());
                    expense.setAmount(rt.getAmount());
                    expense.setCategory(rt.getCategory().getCategoryName());
                    expense.setUser(rt.getUser());
                    expense.setDateTime(LocalDateTime.now());

                    expenseRepository.save(expense);
                } else {
                    Income income = new Income();
                    income.setSource(rt.getTitle());
                    income.setAmount(rt.getAmount());
                    income.setUser(rt.getUser());
                    income.setDateTime(LocalDate.now());

                    incomeRepository.save(income);
                }

                updateNextExecution(rt);

                recurringRepository.save(rt);
                log.info("Recurring processed: {}",rt.getId());
            } catch (Exception e){
                log.error("Error processing recurring id: {}",rt.getId(),e);
            }
        }
        return true;
    }

    private void updateNextExecution(RecurringTransaction rt) {
        LocalDateTime next = rt.getNextExecutionTime();

        switch (rt.getFrequency()){
            case DAILY -> next = next.plusDays(1);
            case WEEKLY -> next = next.plusWeeks(1);
            case MONTHLY -> next = next.plusMonths(1);
        }

        rt.setNextExecutionTime(next);
    }
}
