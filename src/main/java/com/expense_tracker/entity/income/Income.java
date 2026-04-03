package com.expense_tracker.entity.income;

import com.expense_tracker.entity.user.User;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "income")
public class Income {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String source;

    private Double amount;

    private LocalDate dateTime;

    private String description;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
