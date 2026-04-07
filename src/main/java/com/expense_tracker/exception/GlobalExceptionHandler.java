package com.expense_tracker.exception;

import com.expense_tracker.dto.common.ErrorResponse;
import com.expense_tracker.dto.common.Response;
import jakarta.servlet.http.HttpServletRequest;
import org.antlr.v4.runtime.atn.ErrorInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // expense tracker errors
    @ExceptionHandler(ETMException.class)
    public ResponseEntity<ErrorResponse> handleETMException(HttpServletRequest request, ETMException ex){
        log.warn("ETM Exception occurred: [{}] {}",request.getRequestURI(),ex.getMessage(),ex);

        ErrorResponse error = buildError(
                ex.getErrorCode(),
                "ETM Exception",
                ex.getMessage(),
                request.getRequestURI(),
                List.of("Application specific error")
        );

        return new ResponseEntity<>(error, HttpStatus.valueOf(ex.getErrorCode()));
    }

    // validation error
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(HttpServletRequest request,MethodArgumentNotValidException ex){
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() +": "+error.getDefaultMessage())
                .findFirst()
                .orElse("Validation error");

        ErrorResponse response = buildError(
                400,
                "Bad Request",
                errorMessage,
                request.getRequestURI(),
                List.of("Validation failed")
        );
        log.warn("Validation failed: {}",errorMessage,ex);
        return new ResponseEntity<>(response,HttpStatus.BAD_REQUEST);
    }

    //missing param
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParamException(HttpServletRequest request,MissingServletRequestParameterException ex){
        log.warn("Missing request parameter: {}",ex.getParameterName(),ex);
        String message = "Missing required parameter: "+ex.getParameterName();

        ErrorResponse error = buildError(
                400,
                "Bad request",
                message,
                request.getRequestURI(),
                List.of("Parameter is required")
        );

        return new ResponseEntity<>(error,HttpStatus.BAD_REQUEST);
    }

    // type mismatch (query param)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(HttpServletRequest request,MethodArgumentTypeMismatchException ex){

        String paramName = ex.getName();
        Object value = ex.getValue();
        Class<?> requiredType = ex.getRequiredType();

        String message;

        if (requiredType != null && requiredType.isEnum()){
            String allowedValues = Arrays.stream(requiredType.getEnumConstants())
                    .map(Objects::toString)
                    .collect(Collectors.joining(", "));

            message = "Invalid value for '"+ paramName+ "'. Allowed values: "+allowedValues;
        } else if (requiredType !=null){
            message = "Invalid value '"+value+"' for parameter '"+paramName+"'. Expected type: "+requiredType.getSimpleName();
        } else {
            message = "Invalid request parameter: "+paramName;
        }
        log.warn("Type mismatch error: {}",message,ex);

        ErrorResponse error = buildError(
                400,
                "Bad request",
                message,request.getRequestURI(),
                List.of("Check request parameters")
        );

        return new ResponseEntity<>(error,HttpStatus.BAD_REQUEST);
    }


    //json parse error
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleJsonParseException(HttpServletRequest request,HttpMessageNotReadableException ex){

        String message = "Invalid request";
        Throwable cause = ex.getCause();

        if (cause instanceof com.fasterxml.jackson.databind.exc.InvalidFormatException ife){
            Class<?> targetType = ife.getTargetType();

            if (targetType.isEnum()){
                Object[] enumConstants = targetType.getEnumConstants();

                String allowedValues = Arrays.stream(enumConstants)
                        .map(Object::toString)
                        .collect(Collectors.joining(", "));

                message = "Invalid value. Allowed values: "+allowedValues;
            }
        }
        log.warn("Json parse error: {}",message,ex);

        ErrorResponse error = buildError(
                400,
                "Bad request",
                message,
                request.getRequestURI(),
                List.of("Malform JSON")
        );

        return new ResponseEntity<>(error,HttpStatus.BAD_REQUEST);
    }

    // generic exception
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(HttpServletRequest request,Exception ex){
        log.error("Unhandled exception {}",ex.getMessage(),ex);

        ErrorResponse error = buildError(
                500,
                "Internal Server Error",
                "Something went wrong",
                request.getRequestURI(),
                List.of(ex.getMessage())
        );

        return new ResponseEntity<>(error,HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // common builder method
    private ErrorResponse buildError(int status,String error,String message,String path,List<String> details){
        return ErrorResponse.builder()
                .timeStamp(LocalDateTime.now())
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .details(details)
                .build();
    }
}
