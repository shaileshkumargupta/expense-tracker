package com.expense_tracker.controller.goal;

import com.expense_tracker.dto.common.Response;
import com.expense_tracker.dto.goal.GoalContributionRequest;
import com.expense_tracker.dto.goal.GoalRequest;
import com.expense_tracker.dto.goal.GoalResponse;
import com.expense_tracker.service.goal.GoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/goals")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Financial Savings Goals", description = "Operations for managing savings goals, targets, and progress tracking")
@SecurityRequirement(name = "BearerAuth")
public class GoalController {

    private final GoalService goalService;

    @GetMapping
    @Operation(summary = "Get all savings goals", description = "Fetches all active goals for authenticated user with computed percentage completion.")
    public Response getAllGoals(Authentication authentication) {
        String email = authentication.getName();
        log.info("Get all goals request for user: {}", email);

        List<GoalResponse> goals = goalService.getAllGoals(email);

        Response response = new Response();
        response.setSuccessResponse();
        response.setResponse(Map.of("goals", goals));
        return response;
    }

    @PostMapping
    @Operation(summary = "Create a new savings goal", description = "Creates a new savings goal with target amount, category, and target date.")
    public Response createGoal(@Valid @RequestBody GoalRequest request, Authentication authentication) {
        String email = authentication.getName();
        log.info("Create goal request for user: {}, name: {}", email, request.getName());

        GoalResponse createdGoal = goalService.createGoal(request, email);

        Response response = new Response();
        response.setSuccessResponse();
        response.setResponse(Map.of("goal", createdGoal));
        return response;
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a savings goal", description = "Updates details of an existing goal by ID.")
    public Response updateGoal(@PathVariable Long id,
                               @Valid @RequestBody GoalRequest request,
                               Authentication authentication) {
        String email = authentication.getName();
        log.info("Update goal request for goal id: {}, user: {}", id, email);

        GoalResponse updatedGoal = goalService.updateGoal(id, request, email);

        Response response = new Response();
        response.setSuccessResponse();
        response.setResponse(Map.of("goal", updatedGoal));
        return response;
    }

    @PostMapping("/{id}/contribute")
    @Operation(summary = "Contribute to a savings goal", description = "Adds a contribution amount towards the target goal.")
    public Response contributeToGoal(@PathVariable Long id,
                                     @Valid @RequestBody GoalContributionRequest request,
                                     Authentication authentication) {
        String email = authentication.getName();
        log.info("Contribute to goal id: {} amount: {} for user: {}", id, request.getAmount(), email);

        GoalResponse updatedGoal = goalService.contributeToGoal(id, request, email);

        Response response = new Response();
        response.setSuccessResponse();
        response.setResponse(Map.of("goal", updatedGoal));
        return response;
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a savings goal", description = "Deletes a savings goal by ID.")
    public Response deleteGoal(@PathVariable Long id, Authentication authentication) {
        String email = authentication.getName();
        log.info("Delete goal request for id: {} by user: {}", id, email);

        goalService.deleteGoal(id, email);

        Response response = new Response();
        response.setSuccessResponse();
        return response;
    }
}
