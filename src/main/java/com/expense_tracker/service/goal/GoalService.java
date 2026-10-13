package com.expense_tracker.service.goal;

import com.expense_tracker.dto.goal.GoalContributionRequest;
import com.expense_tracker.dto.goal.GoalRequest;
import com.expense_tracker.dto.goal.GoalResponse;

import java.util.List;

public interface GoalService {
    List<GoalResponse> getAllGoals(String email);

    GoalResponse createGoal(GoalRequest request, String email);

    GoalResponse updateGoal(Long id, GoalRequest request, String email);

    GoalResponse contributeToGoal(Long id, GoalContributionRequest request, String email);

    void deleteGoal(Long id, String email);
}
