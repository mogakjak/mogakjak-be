package com.mogakjak.mogakjak.global.websocket.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mogakjak.mogakjak.domain.group.entity.Group;
import com.mogakjak.mogakjak.domain.group.repository.GroupRepository;
import com.mogakjak.mogakjak.domain.user.entity.GroupParticipationStatus;
import com.mogakjak.mogakjak.domain.user.entity.UserGroup;
import com.mogakjak.mogakjak.domain.user.repository.UserGroupRepository;
import com.mogakjak.mogakjak.global.websocket.dto.FocusNotificationDto;
import com.mogakjak.mogakjak.global.websocket.dto.FocusNotificationPublishDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FocusNotificationService {

    private final GroupRepository groupRepository;
    private final UserGroupRepository userGroupRepository;
    private final RedisPubSubService redisPubSubService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 특정 그룹에 대해 집중 체크 알림을 전송
     */
    @Transactional
    public boolean sendFocusNotificationToGroup(UUID groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found: " + groupId));

        Set<UUID> recipientUserIds = getRecipientUserIdsInGroup(group);
        if (recipientUserIds.isEmpty()) {
            return false;
        }

        // 알림 메시지 생성
        FocusNotificationDto notification = FocusNotificationDto.builder()
                .groupId(group.getId())
                .message(group.getNotificationMessage())
                .groupName(group.getName())
                .build();

        try {
            FocusNotificationPublishDto publishDto = FocusNotificationPublishDto.builder()
                    .notification(notification)
                    .recipientUserIds(List.copyOf(recipientUserIds))
                    .build();
            String message = objectMapper.writeValueAsString(publishDto);
            redisPubSubService.publish("focus-notification", message);
            return true;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize focus notification", e);
        }
    }

    /**
     * 그룹 세션에 참여 중이고 개인 수신에 동의한 사용자 ID 목록 조회
     */
    private Set<UUID> getRecipientUserIdsInGroup(Group group) {
        return userGroupRepository.findAllByGroupWithUser(group).stream()
                .filter(ug -> Boolean.FALSE.equals(ug.getUser().getIsDeleted()))
                .filter(ug -> ug.getParticipationStatus() != GroupParticipationStatus.NOT_PARTICIPATING)
                .filter(ug -> Boolean.TRUE.equals(ug.getIsFocusCheckEnabled()))
                .map(userGroup -> userGroup.getUser().getId())
                .collect(Collectors.toSet());
    }

    /**
     * [테스트용] 알림 동의 여부 무시하고 강제 전송. 수신자는 이 그룹에서 참여 중인 사람만 동일하게 적용.
     */
    @Transactional
    public void sendTestNotification(UUID groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found: " + groupId));

        Set<UUID> recipientUserIds = getRecipientUserIdsInGroup(group);

        FocusNotificationDto notification = FocusNotificationDto.builder()
                .groupId(group.getId())
                .message(group.getNotificationMessage() != null && !group.getNotificationMessage().isEmpty()
                    ? group.getNotificationMessage()
                    : "집중 체크 알림")
                .groupName(group.getName())
                .build();

        try {
            FocusNotificationPublishDto publishDto = FocusNotificationPublishDto.builder()
                    .notification(notification)
                    .recipientUserIds(List.copyOf(recipientUserIds))
                    .build();
            String message = objectMapper.writeValueAsString(publishDto);
            redisPubSubService.publish("focus-notification", message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize focus notification", e);
        }
    }
}
