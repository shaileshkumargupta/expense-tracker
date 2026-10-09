package com.expense_tracker.controller.report;

import com.expense_tracker.dto.common.Response;
import com.expense_tracker.dto.report.MonthlyReportResponse;
import com.expense_tracker.exception.ETMException;
import com.expense_tracker.service.report.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Report Management", description = "Endpoints for generating financial and monthly reports")
@SecurityRequirement(name = "BearerAuth")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/monthly")
    @Operation(
            summary = "Get monthly financial report",
            description = "Computes aggregated income, total expenditure, net savings, and category distribution for a specific month and year."
    )
    public Response getMonthlyReport(@RequestParam int month, @RequestParam int year, Authentication authentication){
        String email = authentication.getName();

        log.info("Monthly Report request for user: {}",email);

        if (month < 1 || month > 12)
            throw new ETMException(400,"Invalid month");

        MonthlyReportResponse reportResponse = reportService.getMonthlyReport(email,month,year);
        Response response = new Response();
        response.setSuccessResponse();
        response.setResponse(Map.of("responseData",reportResponse));

        return response;
    }
}
