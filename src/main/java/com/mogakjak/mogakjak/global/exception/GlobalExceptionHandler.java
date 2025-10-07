package com.mogakjak.mogakjak.global.exception;

import com.mogakjak.mogakjak.global.common.ApiResponse;
import com.mogakjak.mogakjak.global.exception.status.StatusCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        StatusCode errorCode = e.getStatusCode();
        log.warn("CustomException occurred: {}", errorCode.getMessage());
        ApiResponse<Void> errorResponse = ApiResponse.error(errorCode);
        return new ResponseEntity<>(errorResponse, errorCode.getHttpStatus());
    }
}
