package com.expense_tracker.controller.user;

import com.expense_tracker.dto.common.Response;
import com.expense_tracker.dto.user.LoginRequest;
import com.expense_tracker.dto.user.RegisterUserRequest;
import com.expense_tracker.dto.user.UpdateUserProfileRequest;
import com.expense_tracker.dto.user.UserSummary;
import com.expense_tracker.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users")
@Tag(name = "User Management", description = "Operations for user registration, authentication, and profile management")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    UserService userService;

    @PostMapping(value = "/registerUser", produces = "application/json")
    @Operation(summary = "Register a new user", description = "Creates a new user profile with encrypted password.")
    public Response registerUser(@Valid @RequestBody RegisterUserRequest request) {
        log.info("Register user request: {}",request);
        userService.registerUser(request);

        Response response = new Response();
        response.setSuccessResponse();
        log.info("Register user response: {}",response);
        return response;
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticates user credentials and returns a JWT Bearer token.")
    public Response login(@RequestBody LoginRequest request){
        log.info("Login user request: {}",request.getEmailId());
        String token = userService.login(request);

        Response response = new Response();
        response.setSuccessResponse();
        response.setResponse(Map.of("token",token));
        log.info("Login user response: responseCode:{}, responseMessage:{}",response.getResponseCode(),response.getResponseMessage());
        return response;
    }

    @GetMapping("/getUserProfile")
    @Operation(summary = "Get user profile", description = "Fetches the profile details of the authenticated user.")
    @SecurityRequirement(name = "BearerAuth")
    public Response getUserProfile(Authentication authentication){
        String email = authentication.getName();
        log.info("Get user profile for: {}",email);

        UserSummary userSummary = userService.getUserProfile(email);

        Response response = new Response();
        response.setSuccessResponse();
        response.setResponse(Map.of("userProfile",userSummary));
        log.info("Get user profile response: {}",response);
        return response;
    }

    @PutMapping("/updateUserProfile")
    @Operation(summary = "Update user profile", description = "Updates the profile details (name, mobile number) of the authenticated user.")
    @SecurityRequirement(name = "BearerAuth")
    public Response updateUserProfile(@Valid @RequestBody UpdateUserProfileRequest request, Authentication authentication) {
        String email = authentication.getName();
        log.info("Update user profile request for user: {}, payload: {}", email, request);

        UserSummary updatedProfile = userService.updateUserProfile(email, request);

        Response response = new Response();
        response.setSuccessResponse();
        response.setResponse(Map.of("userProfile", updatedProfile));
        log.info("Update user profile response: {}", response);
        return response;
    }

}
