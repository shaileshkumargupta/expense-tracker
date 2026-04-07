package com.expense_tracker.service.category.Implementation;

import com.expense_tracker.dto.category.CategoryRequest;
import com.expense_tracker.dto.category.CategoryResponse;
import com.expense_tracker.entity.category.Category;
import com.expense_tracker.entity.category.CategoryType;
import com.expense_tracker.entity.user.User;
import com.expense_tracker.exception.ETMConstantMessages;
import com.expense_tracker.exception.ETMException;
import com.expense_tracker.repository.category.CategoryRepository;
import com.expense_tracker.repository.user.UserRepository;
import com.expense_tracker.service.category.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    private User getUserByEmail(String email){
        return userRepository.findByEmailId(email)
                .orElseThrow(()-> new ETMException(ETMConstantMessages.USER_NOT_FOUND_CODE,ETMConstantMessages.USER_NOT_FOUND));
    }

    @Override
    public void createCategory(CategoryRequest request, String email) {
        User user = getUserByEmail(email);

        if (categoryRepository.existsByUserAndCategoryNameAndType(user,request.getCategoryName(),request.getType())){
            throw new ETMException(ETMConstantMessages.CATEGORY_NOT_FOUND_CODE,ETMConstantMessages.CATEGORY_NOT_FOUND);
        }

        Category category = new Category();
        category.setCategoryName(request.getCategoryName());
        category.setType(request.getType());
        category.setUser(user);

        categoryRepository.save(category);
    }

    @Override
    public List<CategoryResponse> getCategories(CategoryType type, String email) {
        User user = getUserByEmail(email);
        List<Category> categories = categoryRepository.findByUserAndType(user,type);

        return categories.stream()
                .map(cat ->{
                    CategoryResponse res = new CategoryResponse();
                    res.setCategoryId(cat.getCategoryId());
                    res.setCategoryName(cat.getCategoryName());
                    res.setType(cat.getType());
                    return res;
                }).toList();
    }
}
