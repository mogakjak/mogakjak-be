package com.mogakjak.mogakjak.global.websocket.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mogakjak.mogakjak.domain.group.entity.Group;
import com.mogakjak.mogakjak.domain.group.repository.GroupRepository;
import com.mogakjak.mogakjak.domain.lounge.service.OfficialLoungePresenceService;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.repository.UserRepository;
import com.mogakjak.mogakjak.global.exception.status.ErrorCode;
import com.mogakjak.mogakjak.global.websocket.dto.FocusNotificationDto;
import com.mogakjak.mogakjak.global.websocket.dto.FocusNotificationPublishDto;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OfficialLoungeFocusNotificationService {

    private static final String FOCUS_NOTIFICATION_CHANNEL = "focus-notification";
    private static final String DEFAULT_MESSAGE = "공식 라운지 집중 체크 시간이 되었어요.";

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final OfficialLoungePresenceService officialLoungePresenceService;
    private final RealtimeEventPublisher realtimeEventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public void sendHourlyFocusNotification() {
        Group lounge = getOfficialLounge();
        List<UUID> memberIds = officialLoungePresenceService.findAllUserIds();
        if (memberIds.isEmpty()) {
            log.debug("공식 라운지 집중 체크 스케줄러 실행했지만 접속자가 없습니다.");
            return;
        }

        Map<UUID, User> usersById = userRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        List<UUID> recipientUserIds = new ArrayList<>();
        for (UUID memberId : memberIds) {
            User user = usersById.get(memberId);
            if (user == null || Boolean.TRUE.equals(user.getIsDeleted())) {
                officialLoungePresenceService.remove(memberId);
                continue;
            }

            if (Boolean.TRUE.equals(user.getIsOfficialLoungeFocusCheckEnabled())) {
                recipientUserIds.add(memberId);
            }
        }

        if (recipientUserIds.isEmpty()) {
            log.debug("공식 라운지 집중 체크 대상자가 없습니다. loungeId={}", lounge.getId());
            return;
        }

        FocusNotificationDto notification = FocusNotificationDto.builder()
                .groupId(lounge.getId())
                .groupName(lounge.getName())
                .message(resolveMessage(lounge))
                .build();

        try {
            FocusNotificationPublishDto publishDto = FocusNotificationPublishDto.builder()
                    .notification(notification)
                    .recipientUserIds(List.copyOf(recipientUserIds))
                    .build();
            String payload = objectMapper.writeValueAsString(publishDto);
            realtimeEventPublisher.publish(FOCUS_NOTIFICATION_CHANNEL, payload);
            log.info("공식 라운지 집중 체크 발송 완료: loungeId={}, recipientCount={}",
                    lounge.getId(), recipientUserIds.size());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize official lounge focus notification", e);
        }
    }

    private Group getOfficialLounge() {
        return groupRepository.findFirstByIsOfficialLoungeTrue()
                .orElseThrow(() -> new RuntimeException(ErrorCode.OFFICIAL_LOUNGE_NOT_FOUND.getMessage()));
    }

    private String resolveMessage(Group lounge) {
        if (lounge.getNotificationMessage() != null && !lounge.getNotificationMessage().isBlank()) {
            return lounge.getNotificationMessage();
        }
        return DEFAULT_MESSAGE;
    }
}
