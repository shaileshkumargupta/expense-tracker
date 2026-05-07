package com.expense_tracker.repository.expense;

import com.expense_tracker.entity.expense.Expense;
import com.expense_tracker.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByUser(User user);

    @Query("""
    SELECT e FROM Expense e WHERE e.user = :user
        AND MONTH(e.dateTime) = :month
        AND YEAR(e.dateTime) = :year
    """)
    List<Expense> findByUserAndMonthAndYear(@Param("user") User user,@Param("month") int month,@Param("year") int year);
}
