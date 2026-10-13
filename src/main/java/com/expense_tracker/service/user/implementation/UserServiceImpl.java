package com.expense_tracker.service.user.implementation;

import com.expense_tracker.config.jwt.JwtUtil;
import com.expense_tracker.dto.user.LoginRequest;
import com.expense_tracker.dto.user.RegisterUserRequest;
import com.expense_tracker.dto.user.UpdateUserProfileRequest;
import com.expense_tracker.dto.user.UserSummary;
import com.expense_tracker.entity.user.User;
import com.expense_tracker.exception.ETMConstantMessages;
import com.expense_tracker.exception.ETMException;
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
            throw new ETMException(ETMConstantMessages.USER_ALREADY_EXISTS_CODE,ETMConstantMessages.USER_ALREADY_EXISTS);
        }

        User newUser = new User();
        newUser.setName(request.getName());
        newUser.setEmailId(request.getEmailId());
        newUser.setMobileNumber(request.getMobileNumber());

        if (request.getCurrency() != null && !request.getCurrency().trim().isEmpty()) {
            newUser.setCurrency(request.getCurrency().trim().toUpperCase());
        } else {
            newUser.setCurrency("INR");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        newUser.setPassword(encodedPassword);
        userRepository.save(newUser);
        log.info("User saved successfully with email: {} {}", request.getEmailId(),request.getName());
    }

    @Override
    public String login(LoginRequest request) {
        User user = userRepository.findByEmailId(request.getEmailId())
                .orElseThrow(()-> new ETMException(ETMConstantMessages.USER_NOT_FOUND_CODE,ETMConstantMessages.USER_NOT_FOUND));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())){
            throw new ETMException(ETMConstantMessages.INVALID_PASSWORD_CODE,ETMConstantMessages.INVALID_PASSWORD);
        }
        return jwtUtil.generateToken(user.getEmailId());
    }

    @Override
    public UserSummary getUserProfile(String email) {
        User user = userRepository.findByEmailId(email)
                .orElseThrow(()-> new ETMException(ETMConstantMessages.USER_NOT_FOUND_CODE,ETMConstantMessages.USER_NOT_FOUND));

        UserSummary userSummary = new UserSummary();
        userSummary.setUserId(user.getId());
        userSummary.setName(user.getName());
        userSummary.setEmail(user.getEmailId());
        userSummary.setMobileNumber(user.getMobileNumber());
        userSummary.setCurrency(user.getCurrency());

        return userSummary;
    }

    @Override
    @Transactional
    public UserSummary updateUserProfile(String email, UpdateUserProfileRequest request) {
        log.info("Updating user profile for email: {}", email);
        User user = userRepository.findByEmailId(email)
                .orElseThrow(() -> new ETMException(ETMConstantMessages.USER_NOT_FOUND_CODE, ETMConstantMessages.USER_NOT_FOUND));

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            user.setName(request.getName().trim());
        }

        if (request.getMobileNumber() != null && !request.getMobileNumber().trim().isEmpty()) {
            user.setMobileNumber(request.getMobileNumber().trim());
        }

        if (request.getCurrency() != null && !request.getCurrency().trim().isEmpty()) {
            user.setCurrency(request.getCurrency().trim().toUpperCase());
        }

        userRepository.save(user);
        log.info("User profile updated successfully for: {}", email);

        UserSummary updatedSummary = new UserSummary();
        updatedSummary.setUserId(user.getId());
        updatedSummary.setName(user.getName());
        updatedSummary.setEmail(user.getEmailId());
        updatedSummary.setMobileNumber(user.getMobileNumber());
        updatedSummary.setCurrency(user.getCurrency());

        return updatedSummary;
    }
}
