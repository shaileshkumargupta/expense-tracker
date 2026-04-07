package com.expense_tracker.repository.category;

import com.expense_tracker.entity.category.Category;
import com.expense_tracker.entity.category.CategoryType;
import com.expense_tracker.entity.user.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category,Long> {
    boolean existsByUserAndCategoryNameAndType(User user, @NotBlank(message = "Category name is required") String categoryName, @NotNull(message = "Category type is required") CategoryType type);

    List<Category> findByUserAndType(User user, CategoryType type);
}
