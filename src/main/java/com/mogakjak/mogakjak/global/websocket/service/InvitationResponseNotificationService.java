package com.mogakjak.mogakjak.global.websocket.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mogakjak.mogakjak.domain.invitation.entity.Invitation;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.global.websocket.dto.InvitationResponseNotificationDto;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvitationResponseNotificationService {

    private final RealtimeEventPublisher realtimeEventPublisher;
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Transactional
    public void sendInvitationResponse(Invitation invitation, String status) {
        User inviter = invitation.getInviter();
        User invitee = invitation.getInvitee();

        if (Boolean.TRUE.equals(inviter.getIsDeleted())) {
            return;
        }

        String message = status.equals("ACCEPTED")
                ? String.format("%s님이 \"%s\" 초대를 수락했어요!", invitee.getName(), invitation.getGroup().getName())
                : String.format("%s님이 \"%s\" 초대를 거절했어요.", invitee.getName(), invitation.getGroup().getName());

        InvitationResponseNotificationDto dto = InvitationResponseNotificationDto.builder()
                .invitationId(invitation.getId())
                .groupId(invitation.getGroup().getId())
                .groupName(invitation.getGroup().getName())
                .inviterId(inviter.getId())
                .inviteeId(invitee.getId())
                .inviteeNickname(invitee.getName())
                .status(status)
                .message(message)
                .build();

        try {
            String json = objectMapper.writeValueAsString(dto);
            realtimeEventPublisher.publish("invitation-response", json);
            log.debug("초대 응답 알림 전송: invitationId={}, inviterId={}, status={}",
                    invitation.getId(), inviter.getId(), status);
        } catch (JsonProcessingException e) {
            log.error("초대 응답 알림 전송 실패: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send invitation response notification", e);
        }
    }
}
