package com.expense_tracker.repository.user;

import com.expense_tracker.entity.user.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    boolean existsByEmailId(@NotEmpty(message = "Email can not be null or empty") @Email(message = "Invalid email format") String emailId);

    Optional<User> findByEmailId(String emailId);
}
