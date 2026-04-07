package com.expense_tracker.service.income.implementation;

import com.expense_tracker.dto.income.IncomeRequest;
import com.expense_tracker.dto.income.IncomeResponse;
import com.expense_tracker.dto.income.IncomeSummaryResponse;
import com.expense_tracker.dto.income.UpdateIncomeRequest;
import com.expense_tracker.entity.income.Income;
import com.expense_tracker.entity.user.User;
import com.expense_tracker.exception.ETMConstantMessages;
import com.expense_tracker.exception.ETMException;
import com.expense_tracker.repository.income.IncomeRepository;
import com.expense_tracker.repository.user.UserRepository;
import com.expense_tracker.service.income.IncomeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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
                .orElseThrow(()-> new ETMException(ETMConstantMessages.USER_NOT_FOUND_CODE,ETMConstantMessages.USER_NOT_FOUND));
    }

    @Override
    public void addIncome(IncomeRequest request, String email) {
        User user = getUserByEmail(email);

        Income income = new Income();
        income.setSource(request.getSource());
        income.setAmount(request.getAmount());
        income.setDescription(request.getDescription());
        income.setDateTime(request.getDateTime() != null ? request.getDateTime() : LocalDate.now());
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
                .orElseThrow(()-> new ETMException(ETMConstantMessages.INCOME_NOT_FOUND_CODE,ETMConstantMessages.INCOME_NOT_FOUND));

        if (!income.getUser().getId().equals(user.getId())){
            log.warn("Unauthorized update attempt by user: {}",email);
            throw new ETMException(ETMConstantMessages.NOT_ALLOWED_TO_UPDATE_CODE,ETMConstantMessages.NOT_ALLOWED_TO_UPDATE);
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
                .orElseThrow(()->new ETMException(ETMConstantMessages.INCOME_NOT_FOUND_CODE,ETMConstantMessages.INCOME_NOT_FOUND));

        if (!income.getUser().getId().equals(user.getId())){
            log.warn("Unauthorized delete attempt by user: {}",email);
            throw new ETMException(ETMConstantMessages.UNAUTHORIZED_CODE,ETMConstantMessages.UNAUTHORIZED);
        }
        incomeRepository.delete(income);
    }

    @Override
    public IncomeSummaryResponse getIncomeSummaryReport(String email) {
        User user = getUserByEmail(email);

        List<Income> incomes = incomeRepository.findByUser(user);

        double totalAmount = 0;
        Map<String,Double> sourceMap = new HashMap<>();

        for (Income income : incomes){
            totalAmount += income.getAmount();
            sourceMap.put(income.getSource(),
                    sourceMap.getOrDefault(income.getSource(),0.0)+ income.getAmount());
        }

        IncomeSummaryResponse response = new IncomeSummaryResponse();
        response.setTotalAmount(totalAmount);
        response.setTotalTransaction(incomes.size());
        response.setSourceSummary(sourceMap);

        return response;
    }
}
