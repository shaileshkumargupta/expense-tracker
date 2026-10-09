package com.expense_tracker.controller.expense;

import com.expense_tracker.dto.expense.ExpenseResponse;
import com.expense_tracker.service.expense.ExpenseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@Slf4j
public class ExpenseGraphQLController {

    @Autowired
    private ExpenseService expenseService;

    @QueryMapping
    public Map<String, Object> getAllExpense(Authentication authentication) {
        String email = authentication.getName();
        log.info("[GraphQL] getAllExpense for user: {}", email);

        List<ExpenseResponse> expenses = expenseService.getExpenses(email);

        List<Map<String,Object>> expenseList = expenses.stream()
                .map(e -> {
                    Map<String,Object> map = new HashMap<>();
                    map.put("id",e.getId());
                    map.put("title",e.getTitle());
                    map.put("amount",e.getAmount());
                    map.put("category",e.getCategory());
                    map.put("description",e.getDescription());
                    map.put("dateTime",e.getDateTime() != null ? e.getDateTime().toString() : null);
                    return map;
                }).toList();

        return Map.of("status","SUCCESS",
                "message","Expense fetched successfully",
                "expenses",expenseList);
    }
}
