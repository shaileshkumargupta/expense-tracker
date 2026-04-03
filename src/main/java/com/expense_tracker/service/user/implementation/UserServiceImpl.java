package com.expense_tracker.service.user.implementation;

import com.expense_tracker.config.jwt.JwtUtil;
import com.expense_tracker.dto.user.LoginRequest;
import com.expense_tracker.dto.user.RegisterUserRequest;
import com.expense_tracker.dto.user.UserSummary;
import com.expense_tracker.entity.user.User;
import com.expense_tracker.exception.UserAlreadyExistsException;
import com.expense_tracker.exception.UserNotFoundException;
import com.expense_tracker.repository.user.UserRepository;
import com.expense_tracker.service.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private final static Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    @Transactional
    public void registerUser(RegisterUserRequest request) {
        log.info("Checking if user exists with email: {}",request.getEmailId());

        if (userRepository.existsByEmailId(request.getEmailId())){
            log.warn("User already exists with email: {}",request.getEmailId());
            throw new UserAlreadyExistsException("Email already exists!");
        }

        User newUser = new User();
        newUser.setName(request.getName());
        newUser.setEmailId(request.getEmailId());
        newUser.setMobileNumber(request.getMobileNumber());

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        newUser.setPassword(encodedPassword);
        userRepository.save(newUser);
        log.info("User saved successfully with email: {} {}", request.getEmailId(),request.getName());
    }

    @Override
    public String login(LoginRequest request) {
        User user = userRepository.findByEmailId(request.getEmailId())
                .orElseThrow(()-> new RuntimeException("User not found"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())){
            throw new RuntimeException("Invalid password");
        }
        return jwtUtil.generateToken(user.getEmailId());
    }

    @Override
    public UserSummary getUserProfile(String email) {
        User user = userRepository.findByEmailId(email)
                .orElseThrow(()-> new UserNotFoundException("User not found"));

        UserSummary userSummary = new UserSummary();
        userSummary.setUserId(user.getId());
        userSummary.setName(user.getName());
        userSummary.setEmail(user.getEmailId());
        userSummary.setMobileNumber(user.getMobileNumber());

        return userSummary;
    }
}
