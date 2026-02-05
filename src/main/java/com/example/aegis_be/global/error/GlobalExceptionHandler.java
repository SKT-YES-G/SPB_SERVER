package com.example.aegis_be.global.error;

import com.example.aegis_be.global.common.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("BusinessException: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode, e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e) {
        log.warn("Validation failed: {}", e.getMessage());
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse(ErrorCode.INVALID_INPUT_VALUE.getMessage());
        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT_VALUE, message));
    }

    @ExceptionHandler(BindException.class)
    protected ResponseEntity<ApiResponse<Void>> handleBindException(BindException e) {
        log.warn("Bind failed: {}", e.getMessage());
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse(ErrorCode.INVALID_INPUT_VALUE.getMessage());
        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT_VALUE, message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException e) {
        log.warn("Constraint violation: {}", e.getMessage());
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse(ErrorCode.INVALID_INPUT_VALUE.getMessage());
        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT_VALUE, message));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    protected ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e) {
        log.warn("Method not allowed: {}", e.getMessage());
        return ResponseEntity
                .status(ErrorCode.METHOD_NOT_ALLOWED.getStatus())
                .body(ApiResponse.error(ErrorCode.METHOD_NOT_ALLOWED));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    protected ResponseEntity<ApiResponse<Void>> handleNoHandlerFoundException(NoHandlerFoundException e) {
        log.warn("No handler found: {}", e.getMessage());
        return ResponseEntity
                .status(ErrorCode.RESOURCE_NOT_FOUND.getStatus())
                .body(ApiResponse.error(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unexpected error occurred", e);
        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
