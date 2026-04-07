package com.expense_tracker.service.user;

import com.expense_tracker.dto.user.LoginRequest;
import com.expense_tracker.dto.user.RegisterUserRequest;
import com.expense_tracker.dto.user.UserSummary;

public interface UserService {
    void registerUser(RegisterUserRequest request);

    String login(LoginRequest request);

    UserSummary getUserProfile(String email);
}
