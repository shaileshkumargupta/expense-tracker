package com.expense_tracker.dto.user;

import lombok.Data;

@Data
public class UserSummary {
    private Long userId;
    private String name;
    private String email;
    private String mobileNumber;
}
