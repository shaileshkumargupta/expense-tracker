package com.expense_tracker.scheduler;

import com.expense_tracker.service.recurringTxn.impl.RecurringTxnServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class RecurringScheduler implements SchedulerJob{

    private final RecurringTxnServiceImpl recurringTxnService;

    private LocalDateTime nextRunTime = LocalDateTime.now();

    @Override
    public String getJobName() {
        return "RECURRING_JOB";
    }

    @Override
    public LocalDateTime getNextRunTime() {
        return nextRunTime;
    }

    @Override
    public boolean execute() {
        return recurringTxnService.processRecurringTxn();
    }

    @Override
    public void updateNextRunTime() {
        nextRunTime = LocalDateTime.now().plusMinutes(1);
    }
}
