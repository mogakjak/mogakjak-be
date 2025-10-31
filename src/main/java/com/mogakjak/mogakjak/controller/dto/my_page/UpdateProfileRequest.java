package com.mogakjak.mogakjak.controller.dto.my_page;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateProfileRequest {

    @NotBlank(message = "닉네임은 필수 입력값입니다.")
    @Size(max = 20, message = "닉네임은 20자 이하로 입력해주세요.")
    private String nickname;
}