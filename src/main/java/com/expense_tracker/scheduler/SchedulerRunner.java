package com.expense_tracker.scheduler;

import com.expense_tracker.entity.scheduler.SchedulerHistory;
import com.expense_tracker.repository.scheduler.SchedulerHistoryRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class SchedulerRunner {

    private final List<SchedulerJob> jobs;
    private final SchedulerHistoryRepo historyRepository;

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.MINUTES)
    public void runJobs(){
        LocalDateTime now = LocalDateTime.now();

        for (SchedulerJob job : jobs){
            if (job.getNextRunTime() == null || job.getNextRunTime().isAfter(now)){
                continue;
            }

            try {
                log.info("Running job: {}",job.getJobName());

                boolean executed = job.execute();
                job.updateNextRunTime();

                if (executed){
                    SchedulerHistory history = new SchedulerHistory();
                    history.setJobName(job.getJobName());
                    history.setStartTime(now);
                    history.setEndTime(LocalDateTime.now());
                    history.setStatus("SUCCESS");

                    historyRepository.save(history);
                }
            } catch (Exception e){
                SchedulerHistory history = new SchedulerHistory();
                history.setJobName(job.getJobName());
                history.setStartTime(now);
                history.setEndTime(LocalDateTime.now());
                history.setStatus("FAILED");
                history.setErrorMessage(e.getMessage());

                historyRepository.save(history);

                log.error("Job failed: {}",job.getJobName(),e);
            }
        }
    }
}
