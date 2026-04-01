package com.expense_tracker.service.user;

import com.expense_tracker.dto.user.LoginRequest;
import com.expense_tracker.dto.user.RegisterUserRequest;

public interface UserService {
    void registerUser(RegisterUserRequest request);

    String login(LoginRequest request);
}
