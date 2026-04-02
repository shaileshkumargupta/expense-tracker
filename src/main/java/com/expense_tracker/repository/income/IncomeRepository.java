package com.expense_tracker.repository.income;

import com.expense_tracker.entity.income.Income;
import com.expense_tracker.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncomeRepository extends JpaRepository<Income,Long> {

    List<Income> findByUser(User user);
}
