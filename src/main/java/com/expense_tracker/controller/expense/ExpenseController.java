package com.expense_tracker.controller.expense;

import com.expense_tracker.config.jwt.JwtUtil;
import com.expense_tracker.dto.common.Response;
import com.expense_tracker.dto.expense.*;
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
        log.info("Add expense request for user:{}", email);
        expenseService.addExpense(request, email);

        Response response = new Response();
        response.setSuccessResponse();
        log.info("Expense added successfully for user: {} {}", response, email);
        return response;
    }

    @GetMapping("/all")
    @Operation(summary = "Get all expenses", description = "Fetches complete list of recorded expenses for the authenticated user.")
    public Response getExpenses(Authentication authentication){
        String email = authentication.getName();
        log.info("All expense request for user: {}", email);
        List<ExpenseResponse> expenseResponseList = expenseService.getExpenses(email);
        Response response = new Response();
        response.setSuccessResponse();
        response.setResponse(Map.of("expenses", expenseResponseList));
        log.info("All expense response: {}", response);
        return response;
    }

    @PutMapping("/update")
    @Operation(summary = "Update an existing expense", description = "Updates details of a previously recorded expense item using body ID.")
    public Response updateExpense(@Valid @RequestBody UpdateExpenseRequest request, Authentication authentication){
        log.info("Update expense request: {}", request);
        String email = authentication.getName();

        expenseService.updateExpense(request, email);
        Response response = new Response();
        response.setSuccessResponse();
        log.info("Update expense successful for expenseId: {} {}", request.getId(), response);
        return response;
    }

    @PutMapping("/update/{id}")
    @Operation(summary = "Update / Edit expense by ID", description = "Updates details of a previously recorded expense item by path variable ID.")
    public Response updateExpenseById(@PathVariable Long id,
                                      @Valid @RequestBody ExpenseRequest request,
                                      Authentication authentication) {
        String email = authentication.getName();
        log.info("Update expense by id {} request for user: {}, payload: {}", id, email, request);

        ExpenseResponse updatedExpense = expenseService.updateExpenseById(id, request, email);

        Response response = new Response();
        response.setSuccessResponse();
        response.setResponse(Map.of("expense", updatedExpense));
        log.info("Update expense by id successful: {}", response);
        return response;
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "Delete an expense", description = "Removes an expense record by its ID.")
    public Response deleteExpense(@PathVariable Long id, Authentication authentication){
        log.info("Delete expense request for id: {}", id);

        String email = authentication.getName();

        expenseService.deleteExpense(id, email);
        Response response = new Response();
        response.setSuccessResponse();
        log.info("Delete expense successful for expenseId: {} {}", id, response);
        return response;
    }

    @GetMapping("/expenseSummary")
    @Operation(summary = "Get expense summary report", description = "Retrieves category-wise and total expenditure metrics.")
    public Response getExpenseSummaryReport(Authentication authentication){
        String email = authentication.getName();
        log.info("Summary Report for user: {}", email);
        ExpenseSummaryResponse summary = expenseService.getSummaryReport(email);
        Response response = new Response();
        response.setSuccessResponse();
        response.setResponse(Map.of("summary", summary));
        log.info("Summary Report response: {}", response);
        return response;
    }

    @PostMapping("/bulk-delete")
    @Operation(summary = "Bulk delete transactions", description = "Deletes multiple expenses in batch after verifying ownership.")
    public Response bulkDeleteExpenses(@Valid @RequestBody BulkDeleteRequest request, Authentication authentication) {
        String email = authentication.getName();
        log.info("Bulk delete request for user: {}, count: {}", email, request.getExpenseIds().size());

        int deletedCount = expenseService.bulkDeleteExpenses(request.getExpenseIds(), email);

        Response response = new Response();
        response.setSuccessResponse();
        response.setResponse(Map.of("deletedCount", deletedCount));
        log.info("Bulk delete successful for user: {}, deletedCount: {}", email, deletedCount);
        return response;
    }

    @PutMapping("/bulk-categorize")
    @Operation(summary = "Bulk categorize transactions", description = "Updates category across multiple expenses in batch after verifying ownership.")
    public Response bulkCategorizeExpenses(@Valid @RequestBody BulkCategorizeRequest request, Authentication authentication) {
        String email = authentication.getName();
        log.info("Bulk categorize request for user: {}, count: {}, category: {}", email, request.getExpenseIds().size(), request.getCategory());

        int updatedCount = expenseService.bulkCategorizeExpenses(request.getExpenseIds(), request.getCategory(), email);

        Response response = new Response();
        response.setSuccessResponse();
        response.setResponse(Map.of("updatedCount", updatedCount));
        log.info("Bulk categorize successful for user: {}, updatedCount: {}", email, updatedCount);
        return response;
    }

    @GetMapping("/recurring")
    @Operation(summary = "Get recurring commitments & subscriptions", description = "Fetches all recurring expenses and computes total monthly commitment sum.")
    public Response getRecurringExpenses(Authentication authentication) {
        String email = authentication.getName();
        log.info("Get recurring commitments request for user: {}", email);

        RecurringExpensesSummaryResponse summary = expenseService.getRecurringExpenses(email);

        Response response = new Response();
        response.setSuccessResponse();
        response.setResponse(Map.of(
                "subscriptions", summary.getSubscriptions(),
                "totalMonthlyCommitment", summary.getTotalMonthlyCommitment()
        ));
        log.info("Get recurring commitments successful for user: {}, totalMonthlyCommitment: {}", email, summary.getTotalMonthlyCommitment());
        return response;
    }

    @PutMapping("/{id}/recurring")
    @Operation(summary = "Toggle / Set recurring subscription status", description = "Updates recurring flag, frequency, and next due date for an expense.")
    public Response setRecurringStatus(@PathVariable Long id,
                                       @RequestBody SetRecurringRequest request,
                                       Authentication authentication) {
        String email = authentication.getName();
        log.info("Set recurring status request for expense id {}: {}, user: {}", id, request, email);

        ExpenseResponse updatedExpense = expenseService.setRecurringStatus(id, request, email);

        Response response = new Response();
        response.setSuccessResponse();
        response.setResponse(Map.of("expense", updatedExpense));
        log.info("Set recurring status successful for expense id: {}", id);
        return response;
    }
}
