package com.expense_tracker.service.goal.implementation;

import com.expense_tracker.dto.goal.GoalContributionRequest;
import com.expense_tracker.dto.goal.GoalRequest;
import com.expense_tracker.dto.goal.GoalResponse;
import com.expense_tracker.entity.user.User;
import com.expense_tracker.exception.ETMConstantMessages;
import com.expense_tracker.exception.ETMException;
import com.expense_tracker.model.Goal;
import com.expense_tracker.repository.goal.GoalRepository;
import com.expense_tracker.repository.user.UserRepository;
import com.expense_tracker.service.goal.GoalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoalServiceImpl implements GoalService {

    private final GoalRepository goalRepository;
    private final UserRepository userRepository;

    private User getUserByEmail(String email) {
        return userRepository.findByEmailId(email)
                .orElseThrow(() -> new ETMException(ETMConstantMessages.USER_NOT_FOUND_CODE, ETMConstantMessages.USER_NOT_FOUND));
    }

    @Override
    public List<GoalResponse> getAllGoals(String email) {
        User user = getUserByEmail(email);
        List<Goal> goals = goalRepository.findByUserOrderByCreatedAtDesc(user);
        return goals.stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional
    public GoalResponse createGoal(GoalRequest request, String email) {
        User user = getUserByEmail(email);

        Goal goal = Goal.builder()
                .name(request.getName())
                .targetAmount(request.getTargetAmount())
                .currentAmount(request.getCurrentAmount() != null ? request.getCurrentAmount() : 0.0)
                .targetDate(request.getTargetDate())
                .category(request.getCategory())
                .user(user)
                .build();

        Goal saved = goalRepository.save(goal);
        log.info("Created goal '{}' with id: {} for user: {}", saved.getName(), saved.getId(), email);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public GoalResponse updateGoal(Long id, GoalRequest request, String email) {
        User user = getUserByEmail(email);

        Goal goal = goalRepository.findById(id)
                .orElseThrow(() -> new ETMException(ETMConstantMessages.GOAL_NOT_FOUND_CODE, ETMConstantMessages.GOAL_NOT_FOUND));

        if (!goal.getUser().getId().equals(user.getId())) {
            log.warn("Unauthorized attempt to update goal id {} by user: {}", id, email);
            throw new ETMException(ETMConstantMessages.NOT_ALLOWED_TO_UPDATE_CODE, ETMConstantMessages.NOT_ALLOWED_TO_UPDATE);
        }

        goal.setName(request.getName());
        goal.setTargetAmount(request.getTargetAmount());
        if (request.getCurrentAmount() != null) {
            goal.setCurrentAmount(request.getCurrentAmount());
        }
        goal.setTargetDate(request.getTargetDate());
        if (request.getCategory() != null) {
            goal.setCategory(request.getCategory());
        }

        Goal saved = goalRepository.save(goal);
        log.info("Updated goal id: {} for user: {}", saved.getId(), email);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public GoalResponse contributeToGoal(Long id, GoalContributionRequest request, String email) {
        User user = getUserByEmail(email);

        Goal goal = goalRepository.findById(id)
                .orElseThrow(() -> new ETMException(ETMConstantMessages.GOAL_NOT_FOUND_CODE, ETMConstantMessages.GOAL_NOT_FOUND));

        if (!goal.getUser().getId().equals(user.getId())) {
            log.warn("Unauthorized attempt to contribute to goal id {} by user: {}", id, email);
            throw new ETMException(ETMConstantMessages.NOT_ALLOWED_TO_UPDATE_CODE, ETMConstantMessages.NOT_ALLOWED_TO_UPDATE);
        }

        double current = goal.getCurrentAmount() != null ? goal.getCurrentAmount() : 0.0;
        double newAmount = current + request.getAmount();
        goal.setCurrentAmount(newAmount);
        goal.setUpdatedAt(LocalDateTime.now());

        Goal saved = goalRepository.save(goal);
        log.info("Contributed {} to goal id: {}. New amount: {} for user: {}", request.getAmount(), id, newAmount, email);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteGoal(Long id, String email) {
        User user = getUserByEmail(email);

        Goal goal = goalRepository.findById(id)
                .orElseThrow(() -> new ETMException(ETMConstantMessages.GOAL_NOT_FOUND_CODE, ETMConstantMessages.GOAL_NOT_FOUND));

        if (!goal.getUser().getId().equals(user.getId())) {
            log.warn("Unauthorized attempt to delete goal id {} by user: {}", id, email);
            throw new ETMException(ETMConstantMessages.UNAUTHORIZED_CODE, ETMConstantMessages.UNAUTHORIZED);
        }

        goalRepository.delete(goal);
        log.info("Deleted goal id: {} for user: {}", id, email);
    }

    private GoalResponse mapToResponse(Goal goal) {
        double target = goal.getTargetAmount() != null ? goal.getTargetAmount() : 0.0;
        double current = goal.getCurrentAmount() != null ? goal.getCurrentAmount() : 0.0;
        double percentage = target > 0 ? (current / target) * 100.0 : 0.0;
        double roundedPercentage = Math.round(percentage * 100.0) / 100.0;

        return GoalResponse.builder()
                .id(goal.getId())
                .name(goal.getName())
                .targetAmount(target)
                .currentAmount(current)
                .percentageCompleted(roundedPercentage)
                .targetDate(goal.getTargetDate())
                .category(goal.getCategory())
                .createdAt(goal.getCreatedAt())
                .updatedAt(goal.getUpdatedAt())
                .build();
    }
}
