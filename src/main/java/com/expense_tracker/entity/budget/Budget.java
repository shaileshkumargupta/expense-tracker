package com.expense_tracker.entity.budget;

import com.expense_tracker.entity.user.User;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "budget")
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double amount;

    private Integer month;

    private Integer year;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
