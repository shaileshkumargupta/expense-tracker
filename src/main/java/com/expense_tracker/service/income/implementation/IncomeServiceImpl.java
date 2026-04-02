package com.expense_tracker.service.income.implementation;

import com.expense_tracker.dto.income.IncomeRequest;
import com.expense_tracker.dto.income.IncomeResponse;
import com.expense_tracker.dto.income.IncomeSummaryResponse;
import com.expense_tracker.dto.income.UpdateIncomeRequest;
import com.expense_tracker.entity.expense.Expense;
import com.expense_tracker.entity.income.Income;
import com.expense_tracker.entity.user.User;
import com.expense_tracker.exception.ExpenseNotFoundException;
import com.expense_tracker.exception.UserNotFoundException;
import com.expense_tracker.repository.income.IncomeRepository;
import com.expense_tracker.repository.user.UserRepository;
import com.expense_tracker.service.income.IncomeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class IncomeServiceImpl implements IncomeService {

    @Autowired
    private IncomeRepository incomeRepository;

    @Autowired
    private UserRepository userRepository;

    private User getUserByEmail(String email){
        return userRepository.findByEmailId(email)
                .orElseThrow(()-> new UserNotFoundException("User not found"));
    }

    @Override
    public void addIncome(IncomeRequest request, String email) {
        User user = getUserByEmail(email);

        Income income = new Income();
        income.setSource(request.getSource());
        income.setAmount(request.getAmount());
        income.setDescription(request.getDescription());
        income.setDateTime(LocalDateTime.now());
        income.setUser(user);

        incomeRepository.save(income);
    }

    @Override
    public List<IncomeResponse> getIncomes(String email) {
        User user = getUserByEmail(email);

        List<Income> incomes = incomeRepository.findByUser(user);

        return incomes.stream().map(income -> {
            IncomeResponse res = new IncomeResponse();
            res.setId(income.getId());
            res.setSource(income.getSource());
            res.setAmount(income.getAmount());
            res.setDateTime(income.getDateTime());
            res.setDescription(income.getDescription());
            return res;
        }).toList();
    }

    @Override
    public void updateIncome(UpdateIncomeRequest request, String email) {
        User user = getUserByEmail(email);

        Income income = incomeRepository.findById(request.getId())
                .orElseThrow(()-> new RuntimeException("Income not found"));

        if (!income.getUser().getId().equals(user.getId())){
            log.warn("Unauthorized update attempt by user: {}",email);
            throw new RuntimeException("You are not allowed to update this income");
        }

        income.setSource(request.getSource());
        income.setAmount(request.getAmount());
        income.setDescription(request.getDescription());
        incomeRepository.save(income);
    }

    @Override
    public void deleteIncome(Long id, String email) {
        User user = getUserByEmail(email);

        Income income = incomeRepository.findById(id)
                .orElseThrow(()->new RuntimeException("Income not found."));

        if (!income.getUser().getId().equals(user.getId())){
            log.warn("Unauthorized delete attempt by user: {}",email);
            throw new RuntimeException("Not allowed");
        }
        incomeRepository.delete(income);
    }

    @Override
    public IncomeSummaryResponse getIncomeSummaryReport(String email) {
        User user = getUserByEmail(email);

        List<Income> incomes = incomeRepository.findByUser(user);

        double totalAmount = 0;
        Map<String,Double> categoryMap = new HashMap<>();

        for (Income income : incomes){
            totalAmount += income.getAmount();
            categoryMap.put(income.getSource(),
                    categoryMap.getOrDefault(income.getSource(),0.0)+ income.getAmount());
        }

        IncomeSummaryResponse response = new IncomeSummaryResponse();
        response.setTotalAmount(totalAmount);
        response.setTotalTransaction(incomes.size());
        response.setCategorySummary(categoryMap);

        return response;
    }
}
