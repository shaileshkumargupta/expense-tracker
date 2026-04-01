package com.expense_tracker.exception;

import com.expense_tracker.dto.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UserAlreadyExistsException.class)
    public Response handleUserAlreadyException(UserAlreadyExistsException ex){
        Response response = new Response();
        response.setErrorResponse(400,ex.getMessage());
        logger.warn("User already exists: {}",ex.getMessage(),ex);
        return response;
    }

    @ExceptionHandler(UserNotFoundException.class)
    public Response handleUserNotFound(UserNotFoundException ex) {
        Response response = new Response();
        response.setErrorResponse(404, ex.getMessage());
        logger.warn("User not found exception: {}", ex.getMessage(),ex);
        return response;
    }

    @ExceptionHandler(ExpenseNotFoundException.class)
    public Response handleExpenseNotFound(ExpenseNotFoundException ex) {
        Response response = new Response();
        response.setErrorResponse(404, ex.getMessage());
        logger.warn("Expense not found exception: {}", ex.getMessage(),ex);
        return response;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Response handleValidationException(MethodArgumentNotValidException ex){
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() +": "+error.getDefaultMessage())
                .findFirst()
                .orElse("Validation error");

        Response response = new Response();
        response.setErrorResponse(400,errorMessage);
        logger.warn("Validation failed: {}",errorMessage,ex);
        return response;
    }

    @ExceptionHandler(RuntimeException.class)
    public Response handleRuntimeException(RuntimeException ex){
        Response response = new Response();
        response.setErrorResponse(400,ex.getMessage());
        logger.warn(ex.getMessage(),ex);
        return response;
    }

    @ExceptionHandler(Exception.class)
    public Response handleException(Exception ex){
        Response response = new Response();
        response.setErrorResponse(500,"Something went wrong!");
        logger.error("Unhandled exception {}",ex.getMessage(),ex);
        return response;
    }
}
