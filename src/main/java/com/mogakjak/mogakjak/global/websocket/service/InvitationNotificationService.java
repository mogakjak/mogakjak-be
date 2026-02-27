package com.mogakjak.mogakjak.global.websocket.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mogakjak.mogakjak.domain.group.entity.Group;
import com.mogakjak.mogakjak.domain.invitation.entity.Invitation;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.global.websocket.dto.InvitationNotificationDto;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvitationNotificationService {

    private final RedisPubSubService redisPubSubService;
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Transactional
    public void sendInvitationNotification(Invitation invitation) {
        User inviter = invitation.getInviter();
        User invitee = invitation.getInvitee();
        Group group = invitation.getGroup();

        // 탈퇴 유저면 알림 전송 스킵
        if (Boolean.TRUE.equals(invitee.getIsDeleted())) {
            return;
        }

        String message = String.format("%s님이 \"%s\"에 초대했어요!", inviter.getName(), group.getName());

        InvitationNotificationDto dto = InvitationNotificationDto.builder()
                .invitationId(invitation.getId())
                .groupId(group.getId())
                .groupName(group.getName())
                .inviterId(inviter.getId())
                .inviterNickname(inviter.getName())
                .inviteeId(invitee.getId())
                .message(message)
                .build();

        try {
            String json = objectMapper.writeValueAsString(dto);
            redisPubSubService.publish("invitation-notification", json);
            log.debug("초대 알림 전송: invitationId={}, inviteeId={}", invitation.getId(), invitee.getId());
        } catch (JsonProcessingException e) {
            log.error("초대 알림 전송 실패: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send invitation notification", e);
        }
    }
}

