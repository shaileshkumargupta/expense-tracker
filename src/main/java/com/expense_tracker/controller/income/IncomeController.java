package com.expense_tracker.controller.income;

import com.expense_tracker.dto.common.Response;
import com.expense_tracker.dto.income.IncomeRequest;
import com.expense_tracker.dto.income.IncomeResponse;
import com.expense_tracker.dto.income.IncomeSummaryResponse;
import com.expense_tracker.dto.income.UpdateIncomeRequest;
import com.expense_tracker.service.income.IncomeService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/income")
public class IncomeController {

    @Autowired
    private IncomeService incomeService;

    @PostMapping("/add")
    public Response addIncome(@Valid @RequestBody IncomeRequest request, Authentication authentication){
        String email = authentication.getName();
        log.info("Add income request for user: {}",email);

        incomeService.addIncome(request,email);

        Response response = new Response();
        response.setSuccessResponse();
        log.info("Income added successfully: {}",response);
        return response;
    }

    @GetMapping("/all")
    public Response getIncomes(Authentication authentication) {

        String email = authentication.getName();

        log.info("Fetch incomes request for user: {}", email);

        List<IncomeResponse> incomes = incomeService.getIncomes(email);

        Response response = new Response();
        response.setSuccessResponse();
        response.setResponse(Map.of("incomes", incomes));
        log.info("Get all income response: {}",response);
        return response;
    }


    @PutMapping("/update")
    public Response updateExpense(@Valid @RequestBody UpdateIncomeRequest request, Authentication authentication){
        log.info("Update expense request: {}",request);
        String email = authentication.getName();

        incomeService.updateIncome(request,email);
        Response response = new Response();
        response.setSuccessResponse();
        log.info("Update expense successful for expenseId: {} {}",request.getId(),response);
        return response;
    }

    @DeleteMapping("/delete/{id}")
    public Response deleteExpense(@PathVariable Long id, Authentication authentication){
        log.info("Delete expense request for id: {}",id);

        String email = authentication.getName();

        incomeService.deleteIncome(id,email);
        Response response = new Response();
        response.setSuccessResponse();
        log.info("Delete expense successful for expenseId: {} {}",id,response);
        return response;
    }

    @GetMapping("/incomeSummary")
    public Response getIncomeSummaryReport(Authentication authentication){
        String email = authentication.getName();
        log.info("Income Summary Report for user: {}",email);
        IncomeSummaryResponse summary = incomeService.getIncomeSummaryReport(email);
        Response response = new Response();
        response.setSuccessResponse();
        response.setResponse(Map.of("summary",summary));
        log.info("Income Summary Report response: {}",response);
        return response;
    }
}
