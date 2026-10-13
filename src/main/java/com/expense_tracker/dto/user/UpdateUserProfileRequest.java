package com.expense_tracker.dto.user;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateUserProfileRequest {
    private String name;

    @Pattern(regexp = "^[0-9]{10}$", message = "Invalid mobile number. Must be a 10-digit number")
    private String mobileNumber;

    private String currency;
}
