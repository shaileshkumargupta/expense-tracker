package com.expense_tracker.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterUserRequest {
    private String name;
    @NotEmpty(message = "Email can not be null or empty")
    @Email(message = "Invalid email format")
    private String emailId;

    @NotEmpty
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotEmpty(message = "Mobile number can not null or empty")
    @Pattern(regexp = "^[0-9]{10}$",message = "Invalid mobile number")
    private String mobileNumber;

}
