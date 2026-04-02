package com.expense_tracker.controller.expense;

import com.expense_tracker.config.jwt.JwtUtil;
import com.expense_tracker.dto.Response;
import com.expense_tracker.dto.expense.ExpenseRequest;
import com.expense_tracker.dto.expense.ExpenseResponse;
import com.expense_tracker.dto.expense.ExpenseSummaryResponse;
import com.expense_tracker.dto.expense.UpdateExpenseRequest;
import com.expense_tracker.entity.expense.Expense;
import com.expense_tracker.service.expense.ExpenseService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/expenses")
public class ExpenseController {

    private final static Logger log = LoggerFactory.getLogger(ExpenseController.class);

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/add")
    public Response addExpense(@Valid @RequestBody ExpenseRequest request, Authentication authentication){
        String email = authentication.getName();
        log.info("Add expense request for user:{}",email);
        expenseService.addExpense(request,email);

        Response response = new Response();
        response.setSuccessResponse();
        log.info("Expense added successfully for user: {} {}",response,email);
        return response;
    }

    @GetMapping("/all")
    public Response getExpenses(Authentication authentication){
        String email = authentication.getName();
        log.info("All expense request for user: {}",email);
        List<ExpenseResponse> expenseResponseList = expenseService.getExpenses(email);
        Response response = new Response();
        response.setSuccessResponse();
        response.setResponse(Map.of("expenses",expenseResponseList));
        log.info("All expense response: {}",response);
        return response;
    }

    @PutMapping("/update")
    public Response updateExpense(@Valid @RequestBody UpdateExpenseRequest request,Authentication authentication){
        log.info("Update expense request: {}",request);
        String email = authentication.getName();

        expenseService.updateExpense(request,email);
        Response response = new Response();
        response.setSuccessResponse();
        log.info("Update expense successful for expenseId: {} {}",request.getId(),response);
        return response;
    }

    @DeleteMapping("/delete/{id}")
    public Response deleteExpense(@PathVariable Long id, Authentication authentication){
        log.info("Delete expense request for id: {}",id);

        String email = authentication.getName();

        expenseService.deleteExpense(id,email);
        Response response = new Response();
        response.setSuccessResponse();
        log.info("Delete expense successful for expenseId: {} {}",id,response);
        return response;
    }

    @GetMapping("/expenseSummary")
    public Response getExpenseSummaryReport(Authentication authentication){
        String email = authentication.getName();
        log.info("Summary Report for user: {}",email);
        ExpenseSummaryResponse summary = expenseService.getSummaryReport(email);
        Response response = new Response();
        response.setSuccessResponse();
        response.setResponse(Map.of("summary",summary));
        log.info("Summary Report response: {}",response);
        return response;
    }
}
