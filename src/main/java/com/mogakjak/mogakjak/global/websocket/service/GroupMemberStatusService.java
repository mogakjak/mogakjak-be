package com.mogakjak.mogakjak.global.websocket.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mogakjak.mogakjak.domain.group.entity.Group;
import com.mogakjak.mogakjak.domain.group.repository.GroupRepository;
import com.mogakjak.mogakjak.domain.timer.entity.ActiveFocusSession;
import com.mogakjak.mogakjak.domain.timer.entity.FocusInterval;
import com.mogakjak.mogakjak.domain.timer.entity.FocusSession;
import com.mogakjak.mogakjak.domain.timer.repository.ActiveFocusSessionRepository;
import com.mogakjak.mogakjak.domain.timer.repository.FocusIntervalRepository;
import com.mogakjak.mogakjak.domain.timer.repository.FocusSessionRepository;
import com.mogakjak.mogakjak.domain.todo.entity.Todo;
import com.mogakjak.mogakjak.domain.todo.repository.TodoRepository;
import com.mogakjak.mogakjak.domain.user.entity.GroupParticipationStatus;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.entity.UserGroup;
import com.mogakjak.mogakjak.domain.user.repository.UserGroupRepository;
import com.mogakjak.mogakjak.global.websocket.dto.GroupMemberStatusDto;
import com.mogakjak.mogakjak.global.websocket.dto.GroupMemberStatusUpdateDto;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupMemberStatusService {

    private final GroupRepository groupRepository;
    private final UserGroupRepository userGroupRepository;
    private final ActiveFocusSessionRepository activeFocusSessionRepository;
    private final FocusSessionRepository focusSessionRepository;
    private final FocusIntervalRepository focusIntervalRepository;
    private final TodoRepository todoRepository;
    private final RedisPubSubService redisPubSubService;
    
    // ObjectMapper는 JavaTimeModule을 등록한 상태로 초기화
    private ObjectMapper objectMapper;
    
    @PostConstruct
    public void init() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * 그룹의 모든 멤버 상태 조회
     */
    public List<GroupMemberStatusDto> getGroupMemberStatuses(UUID groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found: " + groupId));

        List<UserGroup> userGroups = userGroupRepository.findAllByGroupWithUser(group);
        LocalDateTime now = LocalDateTime.now();

        return userGroups.stream()
                .map(userGroup -> buildMemberStatusDto(userGroup, now))
                .collect(Collectors.toList());
    }

    /**
     * 특정 멤버의 상태만 조회
     */
    public GroupMemberStatusDto getMemberStatus(UUID groupId, UUID userId) {
        UserGroup userGroup = userGroupRepository.findByUser_IdAndGroup_Id(userId, groupId)
                .orElseThrow(() -> new RuntimeException("UserGroup not found"));

        return buildMemberStatusDto(userGroup, LocalDateTime.now());
    }

    /**
     * 그룹 멤버 상태 변경 시 브로드캐스트
     */
    @Transactional
    public void broadcastMemberStatusUpdate(UUID groupId, UUID userId) {
        try {
            GroupMemberStatusDto memberStatus = getMemberStatus(groupId, userId);
            
            GroupMemberStatusUpdateDto updateDto = GroupMemberStatusUpdateDto.builder()
                    .groupId(groupId)
                    .updatedMember(memberStatus)
                    .build();

            String message = objectMapper.writeValueAsString(updateDto);
            redisPubSubService.publish("group-member-status", message);
            
            log.info("그룹 {} 멤버 {} 상태 브로드캐스트 완료", groupId, userId);
            log.info("그룹 멤버 상태 브로드캐스트 - groupId: {}, userId: {}, personalTimerSeconds: {}, todoTitle: {}", 
                    groupId, userId, memberStatus.getPersonalTimerSeconds(), memberStatus.getTodoTitle());
        } catch (JsonProcessingException e) {
            log.error("그룹 멤버 상태 브로드캐스트 실패: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to broadcast member status", e);
        }
    }

    /**
     * 그룹의 모든 멤버 상태 브로드캐스트
     */
    @Transactional
    public void broadcastAllMemberStatuses(UUID groupId) {
        try {
            List<GroupMemberStatusDto> memberStatuses = getGroupMemberStatuses(groupId);
            
            GroupMemberStatusUpdateDto updateDto = GroupMemberStatusUpdateDto.builder()
                    .groupId(groupId)
                    .members(memberStatuses)
                    .build();

            String message = objectMapper.writeValueAsString(updateDto);
            redisPubSubService.publish("group-member-status", message);
            
            log.debug("그룹 {} 전체 멤버 상태 브로드캐스트 완료", groupId);
        } catch (JsonProcessingException e) {
            log.error("그룹 전체 멤버 상태 브로드캐스트 실패: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to broadcast all member statuses", e);
        }
    }

    /**
     * UserGroup으로부터 GroupMemberStatusDto 생성
     */
    private GroupMemberStatusDto buildMemberStatusDto(UserGroup userGroup, LocalDateTime now) {
        User user = userGroup.getUser();
        UUID userId = user.getId();
        UUID groupId = userGroup.getGroup().getId();

        // 개인 타이머 정보 조회
        Optional<ActiveFocusSession> activeSessionOpt = activeFocusSessionRepository.findByUserId(userId);
        Long personalTimerSeconds = null;
        String todoTitle = null;

        if (activeSessionOpt.isPresent()) {
            ActiveFocusSession activeSession = activeSessionOpt.get();
            Optional<FocusSession> focusSessionOpt = focusSessionRepository.findById(activeSession.getSessionId());
            
            if (focusSessionOpt.isPresent()) {
                FocusSession focusSession = focusSessionOpt.get();
                
                // 타이머 누적 시간 공개 여부 확인
                Boolean isTimerPublic = focusSession.getIsTimerPublic();
                if (isTimerPublic == null || isTimerPublic) {
                    // 현재 실행 중인 interval 조회
                    Optional<FocusInterval> currentIntervalOpt = focusIntervalRepository
                            .findTopBySessionIdOrderByStartedAtDesc(focusSession.getId());
                    
                    if (currentIntervalOpt.isPresent()) {
                        FocusInterval currentInterval = currentIntervalOpt.get();
                        LocalDateTime intervalStart = currentInterval.getStartedAt();
                        LocalDateTime intervalEnd = currentInterval.getEndedAt() != null 
                                ? currentInterval.getEndedAt() 
                                : now;
                        
                        // interval 경과 시간 계산
                        long intervalSeconds = Duration.between(intervalStart, intervalEnd).getSeconds();
                        personalTimerSeconds = (focusSession.getTotalDuration() != null ? focusSession.getTotalDuration() : 0L) + intervalSeconds;
                    } else {
                        personalTimerSeconds = focusSession.getTotalDuration() != null ? focusSession.getTotalDuration() : 0L;
                    }
                }
                // isTimerPublic이 false이면 personalTimerSeconds는 null로 유지됨

                // 할일 제목 공개 여부 확인
                Boolean isTaskPublic = focusSession.getIsTaskPublic();
                if ((isTaskPublic == null || isTaskPublic) && focusSession.getTodoId() != null) {
                    Optional<Todo> todoOpt = todoRepository.findById(focusSession.getTodoId());
                    if (todoOpt.isPresent()) {
                        todoTitle = todoOpt.get().getTask();
                    }
                }
                // isTaskPublic이 false이면 todoTitle은 null로 유지됨
            }
        }

        // 최근 참여 며칠 전 계산
        Long daysSinceLastParticipation = null;
        if (userGroup.getEnteredAt() != null) {
            daysSinceLastParticipation = Duration.between(userGroup.getEnteredAt(), now).toDays();
        }

        return GroupMemberStatusDto.builder()
                .groupId(groupId)
                .userId(userId)
                .nickname(user.getName())
                .profileUrl(user.getImageUrl())
                .level(getLevelFromUser(user))
                .participationStatus(userGroup.getParticipationStatus() != null 
                        ? userGroup.getParticipationStatus() 
                        : GroupParticipationStatus.NOT_PARTICIPATING)
                .enteredAt(userGroup.getEnteredAt())
                .daysSinceLastParticipation(daysSinceLastParticipation)
                .personalTimerSeconds(personalTimerSeconds)
                .todoTitle(todoTitle)
                .cheerCount(userGroup.getCheerCount() != null ? userGroup.getCheerCount() : 0)
                .build();
    }

    /**
     * User로부터 레벨 계산
     */
    private Integer getLevelFromUser(User user) {
        if (user.getUserProfile() != null && user.getUserProfile().getMainImageCharacter() != null) {
            return user.getUserProfile().getMainImageCharacter().getLevel();
        }
        return 1;
    }

    /**
     * User로부터 프로필 URL 조회
     */
    private String getProfileUrlFromUser(User user) {
        // 사용자가 설정한 프로필 사진이 있으면 반환
        if (user.getImageUrl() != null) {
            return user.getImageUrl();
        }
        // 없으면 기존 로직대로 캐릭터 이미지 반환
        if (user.getUserProfile() != null && user.getUserProfile().getMainImageCharacter() != null) {
            return user.getUserProfile().getMainImageCharacter().getImageUrl();
        }
        return null;
    }
}

