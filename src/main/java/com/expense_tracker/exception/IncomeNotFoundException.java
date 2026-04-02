package com.expense_tracker.exception;

public class IncomeNotFoundException extends RuntimeException{
    public IncomeNotFoundException(String message){
        super(message);
    }
}
