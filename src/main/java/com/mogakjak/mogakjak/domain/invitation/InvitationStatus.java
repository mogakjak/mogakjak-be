package com.mogakjak.mogakjak.domain.invitation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InvitationStatus {
    PENDING("대기중"),
    ACCEPTED("수락"),
    DECLINED("거절");

    private final String description;
}