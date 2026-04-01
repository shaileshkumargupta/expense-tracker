package com.expense_tracker.dto.user;

import lombok.Data;

@Data
public class LoginRequest {
    private String emailId;
    private String password;
}