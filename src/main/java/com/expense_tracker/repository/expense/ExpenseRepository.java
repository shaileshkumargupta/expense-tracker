package com.expense_tracker.repository.expense;

import com.expense_tracker.entity.expense.Expense;
import com.expense_tracker.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByUser(User user);
}
