package com.expense_tracker.exception;

public class BudgetNotFoundException extends RuntimeException{
    public BudgetNotFoundException(String message){
        super(message);
    }
}
