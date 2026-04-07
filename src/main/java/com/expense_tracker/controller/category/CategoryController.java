package com.expense_tracker.controller.category;

import com.expense_tracker.dto.common.Response;
import com.expense_tracker.dto.category.CategoryRequest;
import com.expense_tracker.dto.category.CategoryResponse;
import com.expense_tracker.entity.category.CategoryType;
import com.expense_tracker.service.category.CategoryService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/category")
@Slf4j
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @PostMapping("/createCategory")
    public Response createCategory(@Valid @RequestBody CategoryRequest request, Authentication authentication){
        String email = authentication.getName();
        log.info("Create category request: {}",request);

        categoryService.createCategory(request,email);

        Response response = new Response();
        response.setSuccessResponse();
        log.info("Category created: {}",response);
        return  response;
    }

    @GetMapping
    public Response getCategories(@RequestParam CategoryType type,Authentication authentication){
        String email = authentication.getName();

        List<CategoryResponse> categories = categoryService.getCategories(type,email);

        Response response = new Response();
        response.setSuccessResponse();
        response.setResponse(Map.of("categories",categories));
        log.info("Get categories response: {}",response);
        return response;
    }
}
