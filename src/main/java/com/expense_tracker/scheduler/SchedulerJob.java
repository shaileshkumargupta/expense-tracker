package com.expense_tracker.scheduler;

import java.time.LocalDateTime;

public interface SchedulerJob {
    String getJobName();

    LocalDateTime getNextRunTime();

    void updateNextRunTime();

    boolean execute();
}
