package com.expense_tracker.dto.common;

import lombok.Data;

@Data
public class CommonResponse {
    protected int responseCode;

    protected String responseMessage;

    public CommonResponse(){};

    public CommonResponse(int responseCode,String responseMessage){
        this();
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
    }

    public void setSuccessResponse(){
        responseCode = 200;
        responseMessage = "Success";
    }

    public void setErrorResponse(int errorCode, String errorMessage){
        this.responseCode = errorCode;
        this.responseMessage = errorMessage;
    }
}
