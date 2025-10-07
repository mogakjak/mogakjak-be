package com.mogakjak.mogakjak.global.common;

import com.mogakjak.mogakjak.global.exception.status.StatusCode;
import com.mogakjak.mogakjak.global.exception.status.SuccessCode;
import lombok.Getter;

@Getter
public class ApiResponse<T> {

    private final int statusCode;
    private final String message;
    private T data;

    private ApiResponse(int statusCode, String message, T data) {
        this.statusCode = statusCode;
        this.message = message;
        this.data = data;
    }

    private ApiResponse(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }

    public static <T> ApiResponse<T> success(SuccessCode successCode, T data) {
        return new ApiResponse<>(successCode.getHttpStatus().value(), successCode.getMessage(), data);
    }

    public static ApiResponse<Void> success(SuccessCode successCode) {
        return new ApiResponse<>(successCode.getHttpStatus().value(), successCode.getMessage());
    }

    public static ApiResponse<Void> error(StatusCode statusCode) {
        return new ApiResponse<>(statusCode.getHttpStatus().value(), statusCode.getMessage());
    }
}
