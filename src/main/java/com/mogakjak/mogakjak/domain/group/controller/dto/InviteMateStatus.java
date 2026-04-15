package com.mogakjak.mogakjak.domain.group.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Schema(description = "초대 가능 상태")
public enum InviteMateStatus {
    CAN_INVITE("초대 가능"),
    ALREADY_INVITED("이미 초대함"),
    ALREADY_GROUP_MEMBER("이미 그룹 멤버");

    private final String description;
}
