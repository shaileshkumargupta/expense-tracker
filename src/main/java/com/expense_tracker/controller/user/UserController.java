package com.expense_tracker.controller.user;

import com.expense_tracker.dto.Response;
import com.expense_tracker.dto.user.LoginRequest;
import com.expense_tracker.dto.user.RegisterUserRequest;
import com.expense_tracker.service.user.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    UserService userService;

    @PostMapping(value = "/registerUser", produces = "application/json")
    public Response registerUser(@Valid @RequestBody RegisterUserRequest request) {
        log.info("Register user request: {}",request);
        userService.registerUser(request);

        Response response = new Response();
        response.setSuccessResponse();
        log.info("Register user response: responseCode:{}, responseMessage:{}",response.getResponseCode(),response.getResponseMessage());
        return response;
    }

    @PostMapping("/login")
    public Response login(@RequestBody LoginRequest request){
        log.info("Login user request: {}",request.getEmailId());
        String token = userService.login(request);

        Response response = new Response();
        response.setSuccessResponse();
        response.setResponse(Map.of("token",token));
        log.info("Login user response: responseCode:{}, responseMessage:{}",response.getResponseCode(),response.getResponseMessage());
        return response;
    }
}
