package com.expense_tracker.controller.dashboard;

import com.expense_tracker.dto.Response;
import com.expense_tracker.dto.dashboard.DashboardResponse;
import com.expense_tracker.service.dashboard.DashboardService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/dashboard")
@Slf4j
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/getAllData")
    public Response getDashBoard(@Valid @RequestParam Integer month, @RequestParam Integer year, Authentication authentication){
        String email = authentication.getName();
        log.info("Get all dashboard data request : {}",email +" month :"+month+", year :"+year);
        DashboardResponse dashboardResponse = dashboardService.getDashBoard(month,year,email);

        Response response = new Response();
        response.setSuccessResponse();
        response.setResponse(Map.of("dashboard",dashboardResponse));
        log.info("Get all dashboard response: {}",response);
        return response;
    }

}
