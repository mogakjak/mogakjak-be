package com.mogakjak.mogakjak.global.exception;

import com.mogakjak.mogakjak.global.exception.status.StatusCode;
import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {

    private final StatusCode statusCode;

    public CustomException(StatusCode statusCode) {
        super(statusCode.getMessage());
        this.statusCode = statusCode;
    }
}
