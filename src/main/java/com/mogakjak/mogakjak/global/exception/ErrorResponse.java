package com.mogakjak.mogakjak.global.exception;

import com.mogakjak.mogakjak.global.exception.status.StatusCode;
import org.springframework.http.ResponseEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {
    private final int status;
    private final String message;

    public static ResponseEntity<ErrorResponse> toResponseEntity(StatusCode statusCode) {
        return ResponseEntity
                .status(statusCode.getHttpStatus())
                .body(ErrorResponse.builder()
                        .status(statusCode.getHttpStatus().value())
                        .message(statusCode.getMessage())
                        .build()
                );
    }
}
