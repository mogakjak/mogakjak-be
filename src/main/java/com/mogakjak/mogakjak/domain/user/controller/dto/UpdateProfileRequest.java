package com.mogakjak.mogakjak.domain.user.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateProfileRequest {

    @Size(max = 20, message = "닉네임은 20자 이하로 입력해주세요.")
    private String nickname;

    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    private String imageUrl;
}