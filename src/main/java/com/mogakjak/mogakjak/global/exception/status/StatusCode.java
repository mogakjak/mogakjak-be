package com.mogakjak.mogakjak.global.exception.status;

import org.springframework.http.HttpStatus;

public interface StatusCode {

    HttpStatus getHttpStatus();
    String getMessage();
}
