package com.mogakjak.mogakjak.domain.group.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HostAckResponse {
    // true면 새 방장이므로 모달을 띄워야 함, false면 띄울 필요 없음 (기존 방장이거나 일반 멤버)
    private boolean needsAcknowledgment;
}