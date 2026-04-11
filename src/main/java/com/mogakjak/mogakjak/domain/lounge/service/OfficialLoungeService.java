package com.mogakjak.mogakjak.domain.lounge.service;

import com.mogakjak.mogakjak.domain.group.entity.Group;
import com.mogakjak.mogakjak.domain.group.repository.GroupRepository;
import com.mogakjak.mogakjak.domain.lounge.dto.OfficialLoungeMemberResponse;
import com.mogakjak.mogakjak.domain.lounge.dto.OfficialLoungeSummaryResponse;
import com.mogakjak.mogakjak.domain.lounge.entity.OfficialLoungeAccessLog;
import com.mogakjak.mogakjak.domain.lounge.repository.OfficialLoungeAccessLogRepository;
import com.mogakjak.mogakjak.domain.quote.dto.QuoteResponse;
import com.mogakjak.mogakjak.domain.quote.service.QuoteService;
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
import com.mogakjak.mogakjak.domain.user.repository.ImageCharacterRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserCharacterRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserRepository;
import com.mogakjak.mogakjak.global.exception.CustomException;
import com.mogakjak.mogakjak.global.exception.status.ErrorCode;
import com.mogakjak.mogakjak.global.websocket.dto.OfficialLoungePresenceUpdateDto;
import com.mogakjak.mogakjak.global.websocket.service.RedisPubSubService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OfficialLoungeService {

    private static final String PRESENCE_CHANNEL = "official-lounge-presence";

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final UserCharacterRepository userCharacterRepository;
    private final ImageCharacterRepository imageCharacterRepository;
    private final QuoteService quoteService;
    private final OfficialLoungePresenceService officialLoungePresenceService;
    private final ActiveFocusSessionRepository activeFocusSessionRepository;
    private final FocusSessionRepository focusSessionRepository;
    private final FocusIntervalRepository focusIntervalRepository;
    private final TodoRepository todoRepository;
    private final OfficialLoungeAccessLogRepository officialLoungeAccessLogRepository;
    private final RedisPubSubService redisPubSubService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public OfficialLoungeSummaryResponse getSummary(UUID userId) {
        Group lounge = getOfficialLounge();
        List<UUID> memberIds = officialLoungePresenceService.findAllUserIds();
        List<OfficialLoungeMemberResponse> members = loadMembers(memberIds);

        return buildResponse(lounge, userId, members, getTodayQuote());
    }

    @Transactional
    public OfficialLoungeSummaryResponse enter(UUID userId) {
        findUserById(userId);
        Group lounge = getOfficialLounge();
        boolean alreadyEntered = isEntered(userId);

        if (!alreadyEntered && !officialLoungePresenceService.enter(userId, lounge.getMaxMemberCount())) {
            long currentMemberCount = getCurrentMemberCount();
            saveAccessLog(lounge.getId(), userId, currentMemberCount, lounge.getMaxMemberCount());
            log.warn("공식 라운지 입장 거절 - userId={}, loungeId={}, currentCount={}, maxCount={}",
                    userId, lounge.getId(), currentMemberCount, lounge.getMaxMemberCount());
            throw new CustomException(ErrorCode.OFFICIAL_LOUNGE_FULL);
        }

        publishPresenceUpdate(lounge.getId(), userId, "ENTER");
        log.info("공식 라운지 입실 완료 - userId={}, loungeId={}", userId, lounge.getId());
        return getSummary(userId);
    }

    @Transactional
    public OfficialLoungeSummaryResponse leave(UUID userId) {
        findUserById(userId);
        Group lounge = getOfficialLounge();
        officialLoungePresenceService.leave(userId);
        publishPresenceUpdate(lounge.getId(), userId, "LEAVE");
        log.info("공식 라운지 퇴실 완료 - userId={}, loungeId={}", userId, lounge.getId());
        return getSummary(userId);
    }

    public boolean isEntered(UUID userId) {
        return officialLoungePresenceService.contains(userId);
    }

    public long getCurrentMemberCount() {
        return officialLoungePresenceService.count();
    }

    @Transactional
    public OfficialLoungeSummaryResponse updateFocusCheck(UUID userId, Boolean enabled) {
        User user = findUserById(userId);
        user.updateOfficialLoungeFocusCheckEnabled(enabled);
        userRepository.save(user);

        Group lounge = getOfficialLounge();
        List<OfficialLoungeMemberResponse> members = loadMembers(officialLoungePresenceService.findAllUserIds());
        return buildResponse(lounge, userId, members, getTodayQuote());
    }

    public void publishPresenceUpdate(UUID loungeId, UUID changedUserId, String eventType) {
        try {
            Group lounge = getOfficialLounge();
            List<UUID> memberIds = officialLoungePresenceService.findAllUserIds();
            List<OfficialLoungeMemberResponse> members = loadMembers(memberIds);
            OfficialLoungePresenceUpdateDto payload = OfficialLoungePresenceUpdateDto.builder()
                    .loungeId(loungeId != null ? loungeId : lounge.getId())
                    .eventType(eventType)
                    .changedUserId(changedUserId)
                    .currentMemberCount((long) members.size())
                    .maxMemberCount(lounge.getMaxMemberCount())
                    .members(members)
                    .build();

            String message = objectMapper.writeValueAsString(payload);
            redisPubSubService.publish(PRESENCE_CHANNEL, message);
        } catch (Exception e) {
            log.error("공식 라운지 presence 브로드캐스트 실패 - loungeId={}, changedUserId={}, eventType={}, error={}",
                    loungeId, changedUserId, eventType, e.getMessage(), e);
        }
    }

    private Group getOfficialLounge() {
        return groupRepository.findFirstByIsOfficialLoungeTrue()
                .orElseThrow(() -> new CustomException(ErrorCode.OFFICIAL_LOUNGE_NOT_FOUND));
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private OfficialLoungeSummaryResponse buildResponse(
            Group lounge,
            UUID userId,
            List<OfficialLoungeMemberResponse> members,
            QuoteResponse todayQuote
    ) {
        return OfficialLoungeSummaryResponse.builder()
                .loungeId(lounge.getId())
                .loungeName(lounge.getName())
                .imageUrl(lounge.getImageUrl())
                .currentMemberCount((long) members.size())
                .maxMemberCount(lounge.getMaxMemberCount())
                .hasEntered(userId != null && isEntered(userId))
                .myFocusCheckEnabled(userId != null && isFocusCheckEnabled(userId))
                .todayQuote(todayQuote)
                .members(members)
                .build();
    }

    private List<OfficialLoungeMemberResponse> loadMembers(List<UUID> memberIds) {
        if (memberIds.isEmpty()) {
            return List.of();
        }

        Map<UUID, User> usersById = userRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        LocalDateTime now = LocalDateTime.now();

        List<OfficialLoungeMemberResponse> members = new ArrayList<>();
        for (UUID memberId : memberIds) {
            User user = usersById.get(memberId);
            if (user == null || Boolean.TRUE.equals(user.getIsDeleted())) {
                officialLoungePresenceService.remove(memberId);
                continue;
            }

            members.add(buildMemberResponse(user, now));
        }

        return members;
    }

    private OfficialLoungeMemberResponse buildMemberResponse(User user, LocalDateTime now) {
        MemberTimerSnapshot timerSnapshot = loadMemberTimerSnapshot(user.getId(), now);

        return OfficialLoungeMemberResponse.builder()
                .userId(user.getId())
                .nickname(user.getName())
                .profileUrl(getProfileUrlFromUser(user))
                .level(getLevelFromUser(user))
                .participationStatus(timerSnapshot.participationStatus().name())
                .lastActiveAt(timerSnapshot.lastActiveAt())
                .personalTimerSeconds(timerSnapshot.personalTimerSeconds())
                .todoTitle(timerSnapshot.todoTitle())
                .build();
    }

    private MemberTimerSnapshot loadMemberTimerSnapshot(UUID userId, LocalDateTime now) {
        Optional<ActiveFocusSession> activeSessionOpt = activeFocusSessionRepository.findByUserId(userId);
        if (activeSessionOpt.isPresent()) {
            ActiveFocusSession activeSession = activeSessionOpt.get();
            Optional<FocusSession> focusSessionOpt = focusSessionRepository.findById(activeSession.getSessionId());
            if (focusSessionOpt.isPresent()) {
                return buildSnapshotFromActiveSession(focusSessionOpt.get(), now);
            }
        }

        Optional<FocusSession> lastSessionOpt = focusSessionRepository.findTopByUserIdOrderByStartedAtDesc(userId);
        LocalDateTime lastActiveAt = lastSessionOpt
                .map(session -> session.getEndedAt() != null ? session.getEndedAt() : session.getStartedAt())
                .orElse(null);

        return new MemberTimerSnapshot(
                GroupParticipationStatus.NOT_PARTICIPATING,
                lastActiveAt,
                null,
                null
        );
    }

    private MemberTimerSnapshot buildSnapshotFromActiveSession(FocusSession focusSession, LocalDateTime now) {
        GroupParticipationStatus participationStatus =
                focusSession.getStatus() == TimerStatus.PAUSED
                        ? GroupParticipationStatus.RESTING
                        : GroupParticipationStatus.PARTICIPATING;

        Long personalTimerSeconds = null;
        if (Boolean.TRUE.equals(focusSession.getIsTimerPublic()) || focusSession.getIsTimerPublic() == null) {
            long total = focusSession.getTotalDuration() != null ? focusSession.getTotalDuration() : 0L;
            if (focusSession.getStatus() == TimerStatus.PAUSED) {
                personalTimerSeconds = total;
            } else {
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

        String todoTitle = null;
        if ((focusSession.getIsTaskPublic() == null || focusSession.getIsTaskPublic())
                && focusSession.getTodoId() != null) {
            Optional<Todo> todoOpt = todoRepository.findById(focusSession.getTodoId());
            if (todoOpt.isPresent()) {
                todoTitle = todoOpt.get().getTask();
            }
        }

        LocalDateTime lastActiveAt = focusSession.getEndedAt() != null
                ? focusSession.getEndedAt()
                : focusSession.getStartedAt();

        return new MemberTimerSnapshot(
                participationStatus,
                lastActiveAt,
                personalTimerSeconds,
                todoTitle
        );
    }

    private record MemberTimerSnapshot(
            GroupParticipationStatus participationStatus,
            LocalDateTime lastActiveAt,
            Long personalTimerSeconds,
            String todoTitle
    ) {
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

    private void saveAccessLog(UUID loungeId, UUID userId, long currentMemberCount, Integer maxMemberCount) {
        try {
            OfficialLoungeAccessLog accessLog = OfficialLoungeAccessLog.builder()
                    .loungeId(loungeId)
                    .userId(userId)
                    .attemptedAt(LocalDateTime.now())
                    .result("DENIED")
                    .reason("official lounge is full")
                    .currentMemberCount(currentMemberCount)
                    .maxMemberCount(maxMemberCount)
                    .build();
            officialLoungeAccessLogRepository.save(accessLog);
        } catch (Exception e) {
            log.error("공식 라운지 입장 거절 로그 저장 실패 - userId={}, loungeId={}, error={}",
                    userId, loungeId, e.getMessage(), e);
        }
    }

    private boolean isFocusCheckEnabled(UUID userId) {
        return userRepository.findById(userId)
                .map(User::getIsOfficialLoungeFocusCheckEnabled)
                .orElse(false);
    }

    private QuoteResponse getTodayQuote() {
        try {
            return quoteService.getRandomQuote();
        } catch (CustomException e) {
            if (e.getStatusCode() == ErrorCode.QUOTE_NOT_FOUND) {
                return null;
            }
            throw e;
        }
    }
}
