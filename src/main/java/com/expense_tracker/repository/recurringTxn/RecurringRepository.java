package com.expense_tracker.repository.recurringTxn;

import com.expense_tracker.entity.recurringTxn.RecurringTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RecurringRepository extends JpaRepository<RecurringTransaction,Long> {
    List<RecurringTransaction> findByNextExecutionTimeBefore(LocalDateTime now);
}
