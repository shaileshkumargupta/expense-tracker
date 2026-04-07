package com.expense_tracker.exception;

import lombok.Getter;

@Getter
public class ETMException extends RuntimeException{
    private final Integer errorCode;

    public ETMException(Integer errorCode,String errorMessage){
        super(errorMessage);
        this.errorCode=errorCode;
    }
}
