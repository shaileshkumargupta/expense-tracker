package com.expense_tracker.repository.goal;

import com.expense_tracker.entity.user.User;
import com.expense_tracker.model.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Long> {
    List<Goal> findByUser(User user);
    List<Goal> findByUserOrderByCreatedAtDesc(User user);
    Optional<Goal> findByIdAndUser(Long id, User user);
}
