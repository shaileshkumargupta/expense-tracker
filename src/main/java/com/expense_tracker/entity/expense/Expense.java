package com.expense_tracker.entity.expense;

import com.expense_tracker.entity.user.User;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "expenses")
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private Double amount;
    private String category;
    private LocalDateTime dateTime;
    private String description;

    @Column(name = "is_recurring")
    private Boolean isRecurring = false;

    @Column(name = "frequency")
    private String frequency = "MONTHLY";

    @Column(name = "next_due_date")
    private LocalDate nextDueDate;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
