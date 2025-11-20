package com.mogakjak.mogakjak.global.websocket.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mogakjak.mogakjak.domain.group.entity.Group;
import com.mogakjak.mogakjak.domain.group.repository.GroupRepository;
import com.mogakjak.mogakjak.domain.timer.repository.ActiveFocusSessionRepository;
import com.mogakjak.mogakjak.domain.user.entity.GroupParticipationStatus;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.entity.UserGroup;
import com.mogakjak.mogakjak.domain.user.repository.UserGroupRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserRepository;
import com.mogakjak.mogakjak.global.websocket.dto.FocusNotificationDto;
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
    private final UserRepository userRepository;
    private final RedisPubSubService redisPubSubService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 특정 그룹에 대해 집중 체크 알림을 전송
     */
    @Transactional
    public void sendFocusNotificationToGroup(UUID groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found: " + groupId));

        // 알림 동의 여부 확인
        if (!group.getIsNotificationAgreed()) {
            return;
        }

        // 활동 중인 사용자 조회
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

        // Redis로 발행 (웹소켓을 통해 전송)
        try {
            String message = objectMapper.writeValueAsString(notification);
            redisPubSubService.publish("focus-notification", message);
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
                .filter(userGroup -> isUserActive(userGroup.getUser(), userGroup))
                .map(userGroup -> userGroup.getUser().getId())
                .collect(Collectors.toSet());
    }

    /**
     * 사용자가 활동 중인지 판단
     * - 개인 타이머 활성 상태: ActiveFocusSession에 유저 존재
     * - 그룹 참여 중 상태: participationStatus가 RESTING 또는 PARTICIPATING이고 enteredAt이 null이 아님
     */
    private boolean isUserActive(User user, UserGroup userGroup) {
        // 개인 타이머 활성 상태 확인
        boolean hasActivePersonalTimer = activeFocusSessionRepository.findByUserId(user.getId()).isPresent();

        // 그룹 참여 중 상태 확인
        boolean isGroupParticipating = userGroup.getParticipationStatus() != GroupParticipationStatus.NOT_PARTICIPATING
                && userGroup.getEnteredAt() != null;

        return hasActivePersonalTimer || isGroupParticipating;
    }

    /**
     * [테스트용] 알림 동의 여부와 활동 중인 사용자 여부를 무시하고 강제로 알림 전송
     */
    @Transactional
    public void sendTestNotification(UUID groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found: " + groupId));

        // 알림 메시지 생성 (알림 동의 여부와 활동 중인 사용자 여부 무시)
        FocusNotificationDto notification = FocusNotificationDto.builder()
                .groupId(group.getId())
                .message(group.getNotificationMessage() != null && !group.getNotificationMessage().isEmpty() 
                    ? group.getNotificationMessage() 
                    : "집중 체크 알림")
                .groupName(group.getName())
                .build();

        // Redis로 발행 (웹소켓을 통해 전송)
        try {
            String message = objectMapper.writeValueAsString(notification);
            redisPubSubService.publish("focus-notification", message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize focus notification", e);
        }
    }
}

