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
import com.mogakjak.mogakjak.domain.timer.enumerate.TimerStatus;
import com.mogakjak.mogakjak.domain.timer.repository.ActiveFocusSessionRepository;
import com.mogakjak.mogakjak.domain.timer.repository.FocusIntervalRepository;
import com.mogakjak.mogakjak.domain.timer.repository.FocusSessionRepository;
import com.mogakjak.mogakjak.domain.todo.entity.Todo;
import com.mogakjak.mogakjak.domain.todo.repository.TodoRepository;
import com.mogakjak.mogakjak.domain.user.entity.GroupParticipationStatus;
import com.mogakjak.mogakjak.domain.user.entity.ImageCharacter;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.entity.UserGroup;
import com.mogakjak.mogakjak.domain.user.repository.ImageCharacterRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserCharacterRepository;
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
    private final RealtimeEventPublisher realtimeEventPublisher;
    private final UserCharacterRepository userCharacterRepository;
    private final ImageCharacterRepository imageCharacterRepository;
    
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

        if (Boolean.TRUE.equals(userGroup.getUser().getIsDeleted())) {
            throw new RuntimeException("User is deleted");
        }

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
            realtimeEventPublisher.publish("group-member-status", message);
            
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
            realtimeEventPublisher.publish("group-member-status", message);
            
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
                    long total = focusSession.getTotalDuration() != null ? focusSession.getTotalDuration() : 0L;
                    // PAUSED면 pause 시점에 이미 마지막 interval이 totalDuration에 반영되어 있음 → 중복 가산 방지
                    if (focusSession.getStatus() == TimerStatus.PAUSED) {
                        personalTimerSeconds = total;
                    } else {
                        // RUNNING 등: 현재 구간 경과 시간을 더해서 실시간 표시
                        Optional<FocusInterval> currentIntervalOpt = focusIntervalRepository
                                .findTopBySessionIdOrderByStartedAtDesc(focusSession.getId());
                        if (currentIntervalOpt.isPresent()) {
                            FocusInterval currentInterval = currentIntervalOpt.get();
                            LocalDateTime intervalStart = currentInterval.getStartedAt();
                            LocalDateTime intervalEnd = currentInterval.getEndedAt() != null
                                    ? currentInterval.getEndedAt()
                                    : now;
                            long intervalSeconds = Duration.between(intervalStart, intervalEnd).getSeconds();
                            personalTimerSeconds = total + intervalSeconds;
                        } else {
                            personalTimerSeconds = total;
                        }
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
//                .profileUrl(user.getImageUrl())
                .profileUrl(getProfileUrlFromUser(user))
                .level(getLevelFromUser(user))
                .role(userGroup.getRole())
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

    private Integer getLevelFromUser(User user) {
        return userCharacterRepository.findTopByUserOrderByImageCharacter_LevelDescImageCharacter_CreatedAtAsc(user)
                .map(uc -> uc.getImageCharacter().getLevel())
                .orElse(1);
    }

    private String getProfileUrlFromUser(User user) {
        if (user.getImageUrl() != null) {
            return user.getImageUrl();
        }
        return userCharacterRepository.findTopByUserOrderByImageCharacter_LevelDescImageCharacter_CreatedAtAsc(user)
                .map(uc -> uc.getImageCharacter().getImageUrl())
                .orElseGet(() ->
                        imageCharacterRepository.findFirstByLevelAndIsActiveTrueOrderByCreatedAtAsc(1)
                                .map(ImageCharacter::getImageUrl)
                                .orElse(null)
                );
    }
}
