package com.expense_tracker.repository.income;

import com.expense_tracker.entity.income.Income;
import com.expense_tracker.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncomeRepository extends JpaRepository<Income,Long> {

    List<Income> findByUser(User user);

    @Query("""
    SELECT i FROM Income i
        WHERE i.user = :user
        AND MONTH(i.dateTime) = :month
        AND YEAR(i.dateTime) = :year
    """)
    List<Income> findByUserAndMonthAndYear(@Param("user") User user,@Param("month") int month,@Param("year") int year);
}
