package com.expense_tracker.repository.budget;

import com.expense_tracker.entity.budget.Budget;
import com.expense_tracker.entity.user.User;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget,Long> {
    Optional<Budget> findByUserAndMonthAndYear(User user, @NotNull(message = "Month is required") Integer month, @NotNull(message = "Year is required") Integer year);
}
