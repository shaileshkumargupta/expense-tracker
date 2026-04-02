package com.expense_tracker.controller.budget;

import com.expense_tracker.dto.Response;
import com.expense_tracker.dto.budget.BudgetRequest;
import com.expense_tracker.dto.budget.BudgetResponse;
import com.expense_tracker.dto.budget.BudgetSummaryResponse;
import com.expense_tracker.service.budget.BudgetService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/budget")
@Slf4j
public class BudgetController {

    @Autowired
    private BudgetService budgetService;

    @PostMapping("/setBudget")
    public Response setBudget(@Valid @RequestBody BudgetRequest request, Authentication authentication){
        String email = authentication.getName();
        log.info("Set budget request: {} {}",request,email);

        budgetService.setBudget(request,email);

        Response response = new Response();
        response.setSuccessResponse();
        log.info("Set budget response: {}",response);
        return response;
    }

    @GetMapping("/getBudget")
    public Response getBudget(@RequestParam Integer month,@RequestParam Integer year, Authentication authentication){
        String email = authentication.getName();

        BudgetResponse budgetResponse = budgetService.getBudget(month,year,email);

        Response response = new Response();
        response.setSuccessResponse();
        response.setResponse(Map.of("budget",budgetResponse));
        log.info("Get budget response: {}",response);
        return response;
    }

    @GetMapping("/getBudgetSummary")
    public Response getBudgetSummary(@RequestParam Integer month, @RequestParam Integer year,Authentication authentication){
        String email = authentication.getName();

        BudgetSummaryResponse summary =
                budgetService.getBudgetSummary(month, year, email);

        Response response = new Response();
        response.setSuccessResponse();
        response.setResponse(Map.of("summary", summary));
        log.info("Get budget summary response: {}",response);
        return response;
    }
}
