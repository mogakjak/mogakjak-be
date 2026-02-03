package com.mogakjak.mogakjak.domain.user.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "개인 정보 동의 요청 DTO")
public class AgreementRequest {

    private boolean termsAgreed;

    private boolean privacyAgreed;

    private boolean marketingAgreed;

}
