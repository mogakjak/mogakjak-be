package com.mogakjak.mogakjak.controller;

import com.mogakjak.mogakjak.global.exception.status.ErrorCode;
import com.mogakjak.mogakjak.global.exception.status.SuccessCode;
import com.mogakjak.mogakjak.global.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/success")
    public ApiResponse<?> helloWorldSuccess() {
        return ApiResponse.success(SuccessCode.OK, "hello-world");
    }

    @GetMapping("/failure")
    public ApiResponse<?> helloWorldFailure() {
        return ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
