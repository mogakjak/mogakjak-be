package com.mogakjak.mogakjak.global.websocket.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mogakjak.mogakjak.domain.group.entity.Group;
import com.mogakjak.mogakjak.domain.group.repository.GroupRepository;
import com.mogakjak.mogakjak.domain.timer.repository.ActiveFocusSessionRepository;
import com.mogakjak.mogakjak.domain.timer.repository.FocusSessionRepository;
import com.mogakjak.mogakjak.domain.user.entity.GroupParticipationStatus;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.entity.UserGroup;
import com.mogakjak.mogakjak.domain.user.repository.UserGroupRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserRepository;
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
    private final ActiveFocusSessionRepository activeFocusSessionRepository;
    private final FocusSessionRepository focusSessionRepository;
    private final UserRepository userRepository;
    private final RealtimeEventPublisher realtimeEventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 특정 그룹에 대해 집중 체크 알림을 전송
     */
    @Transactional
    public void sendFocusNotificationToGroup(UUID groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found: " + groupId));

        if (!group.getIsNotificationAgreed()) {
            return;
        }

        Set<UUID> activeUserIds = getActiveUserIdsInGroup(groupId);
        if (activeUserIds.isEmpty()) {
            return; // 활동 중인 사용자가 없으면 알림 전송하지 않음
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
                    .recipientUserIds(List.copyOf(activeUserIds))
                    .build();
            String message = objectMapper.writeValueAsString(publishDto);
            realtimeEventPublisher.publish("focus-notification", message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize focus notification", e);
        }
    }

    /**
     * 특정 그룹 내 활동 중인 사용자 ID 목록 조회
     */
    private Set<UUID> getActiveUserIdsInGroup(UUID groupId) {
        // 그룹에 속한 모든 사용자 조회
        List<UserGroup> userGroups = userGroupRepository.findAllByGroupWithUser(
                groupRepository.findById(groupId)
                        .orElseThrow(() -> new RuntimeException("Group not found: " + groupId))
        );

        return userGroups.stream()
                .filter(ug -> Boolean.FALSE.equals(ug.getUser().getIsDeleted()))
                .filter(userGroup -> isUserActiveInGroup(userGroup.getUser(), userGroup, groupId))
                .map(userGroup -> userGroup.getUser().getId())
                .collect(Collectors.toSet());
    }

    /**
     * 해당 그룹에서 "개인 타이머 실행 중"인지 판단
     * - 이 그룹에서 개인 타이머 실행 중: ActiveFocusSession → FocusSession.groupId == groupId
     */
    private boolean isUserActiveInGroup(User user, UserGroup userGroup, UUID groupId) {
        return activeFocusSessionRepository.findByUserId(user.getId())
                .map(afs -> focusSessionRepository.findById(afs.getSessionId()).orElse(null))
                .filter(fs -> fs != null && groupId.equals(fs.getGroupId()))
                .isPresent();
    }

    /**
     * [테스트용] 알림 동의 여부 무시하고 강제 전송. 수신자는 이 그룹에서 참여 중인 사람만 동일하게 적용.
     */
    @Transactional
    public void sendTestNotification(UUID groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found: " + groupId));

        Set<UUID> activeUserIds = getActiveUserIdsInGroup(groupId);

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
                    .recipientUserIds(List.copyOf(activeUserIds))
                    .build();
            String message = objectMapper.writeValueAsString(publishDto);
            realtimeEventPublisher.publish("focus-notification", message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize focus notification", e);
        }
    }
}
