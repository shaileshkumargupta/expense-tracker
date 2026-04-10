package com.expense_tracker.repository.scheduler;

import com.expense_tracker.entity.scheduler.SchedulerHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SchedulerHistoryRepo extends JpaRepository<SchedulerHistory,Long> {
}
