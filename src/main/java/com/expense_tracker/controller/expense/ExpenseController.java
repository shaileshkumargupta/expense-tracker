package com.expense_tracker.controller.expense;

import com.expense_tracker.config.jwt.JwtUtil;
import com.expense_tracker.dto.common.Response;
import com.expense_tracker.dto.expense.ExpenseRequest;
import com.expense_tracker.dto.expense.ExpenseResponse;
import com.expense_tracker.dto.expense.ExpenseSummaryResponse;
import com.expense_tracker.dto.expense.UpdateExpenseRequest;
import com.expense_tracker.service.expense.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/expenses")
@Tag(name = "Expense Management", description = "Operations for tracking and managing user expenses")
@SecurityRequirement(name = "BearerAuth")
public class ExpenseController {

    private final static Logger log = LoggerFactory.getLogger(ExpenseController.class);

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/add")
    @Operation(summary = "Record a new expense", description = "Records an expense entry under a specific category for the authenticated user.")
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
    @Operation(summary = "Get all expenses", description = "Fetches complete list of recorded expenses for the authenticated user.")
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
    @Operation(summary = "Update an existing expense", description = "Updates details of a previously recorded expense item.")
    public Response updateExpense(@Valid @RequestBody UpdateExpenseRequest request, Authentication authentication){
        log.info("Update expense request: {}",request);
        String email = authentication.getName();

        expenseService.updateExpense(request,email);
        Response response = new Response();
        response.setSuccessResponse();
        log.info("Update expense successful for expenseId: {} {}",request.getId(),response);
        return response;
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "Delete an expense", description = "Removes an expense record by its ID.")
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
    @Operation(summary = "Get expense summary report", description = "Retrieves category-wise and total expenditure metrics.")
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
