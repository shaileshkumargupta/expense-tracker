package com.expense_tracker.entity.scheduler;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "scheduler_history")
public class SchedulerHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String jobName;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String status;

    private String errorMessage;
}
