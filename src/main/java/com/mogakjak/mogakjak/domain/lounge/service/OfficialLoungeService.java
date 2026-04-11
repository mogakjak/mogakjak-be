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
import com.mogakjak.mogakjak.domain.user.entity.UserCharacter;
import com.mogakjak.mogakjak.domain.user.repository.ImageCharacterRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserCharacterRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserRepository;
import com.mogakjak.mogakjak.global.exception.CustomException;
import com.mogakjak.mogakjak.global.exception.status.ErrorCode;
import com.mogakjak.mogakjak.global.websocket.dto.OfficialLoungePresenceUpdateDto;
import com.mogakjak.mogakjak.global.websocket.service.CheerNotificationService;
import com.mogakjak.mogakjak.global.websocket.service.RedisPubSubService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;
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
    private final CheerNotificationService cheerNotificationService;
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

    @Transactional
    public void sendCheer(UUID userId, UUID targetUserId) {
        if (userId.equals(targetUserId)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        User sender = findUserById(userId);
        User target = findUserById(targetUserId);
        Group lounge = getOfficialLounge();

        if (!officialLoungePresenceService.contains(sender.getId())
                || !officialLoungePresenceService.contains(target.getId())) {
            throw new CustomException(ErrorCode.OFFICIAL_LOUNGE_MEMBER_NOT_FOUND);
        }

        officialLoungePresenceService.incrementCheerCount(targetUserId);
        cheerNotificationService.sendCheerNotification(userId, targetUserId, lounge.getId());

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronizationAdapter() {
                        @Override
                        public void afterCommit() {
                            publishPresenceUpdate(lounge.getId(), targetUserId, "CHEER");
                        }
                    }
            );
            return;
        }

        publishPresenceUpdate(lounge.getId(), targetUserId, "CHEER");
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
                .filter(user -> !Boolean.TRUE.equals(user.getIsDeleted()))
                .collect(Collectors.toMap(
                        User::getId,
                        user -> user,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        List<UUID> validMemberIds = new ArrayList<>();
        for (UUID memberId : memberIds) {
            User user = usersById.get(memberId);
            if (user == null) {
                officialLoungePresenceService.remove(memberId);
                continue;
            }
            validMemberIds.add(memberId);
        }

        if (validMemberIds.isEmpty()) {
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now();
        MemberLoadContext context = loadMemberLoadContext(validMemberIds);

        List<OfficialLoungeMemberResponse> members = new ArrayList<>();
        for (UUID memberId : validMemberIds) {
            User user = usersById.get(memberId);
            if (user != null) {
                members.add(buildMemberResponse(user, now, context));
            }
        }

        return members;
    }

    private OfficialLoungeMemberResponse buildMemberResponse(User user, LocalDateTime now, MemberLoadContext context) {
        MemberTimerSnapshot timerSnapshot = loadMemberTimerSnapshot(user.getId(), now, context);

        return OfficialLoungeMemberResponse.builder()
                .userId(user.getId())
                .nickname(user.getName())
                .profileUrl(getProfileUrlFromUser(user, context.topUserCharacterByUserId(), context.defaultImageCharacter()))
                .level(getLevelFromUser(user, context.topUserCharacterByUserId()))
                .participationStatus(timerSnapshot.participationStatus().name())
                .enteredAt(timerSnapshot.enteredAt())
                .lastActiveAt(timerSnapshot.lastActiveAt())
                .daysSinceLastParticipation(timerSnapshot.daysSinceLastParticipation())
                .personalTimerSeconds(timerSnapshot.personalTimerSeconds())
                .todoTitle(timerSnapshot.todoTitle())
                .cheerCount(timerSnapshot.cheerCount())
                .build();
    }

    private MemberTimerSnapshot loadMemberTimerSnapshot(UUID userId, LocalDateTime now, MemberLoadContext context) {
        FocusSession latestSession = context.latestSessionByUserId().get(userId);
        boolean hasActiveSession = context.activeSessionUserIds().contains(userId);
        if (hasActiveSession && latestSession != null) {
            return buildSnapshotFromActiveSession(userId, latestSession, now, context);
        }

        LocalDateTime lastActiveAt = latestSession != null
                ? latestSession.getEndedAt() != null ? latestSession.getEndedAt() : latestSession.getStartedAt()
                : null;
        LocalDateTime enteredAt = context.enteredAtByUserId().get(userId);
        if (enteredAt == null) {
            enteredAt = lastActiveAt;
        }
        Long daysSinceLastParticipation = enteredAt != null ? Duration.between(enteredAt, now).toDays() : null;

        return new MemberTimerSnapshot(
                GroupParticipationStatus.NOT_PARTICIPATING,
                lastActiveAt,
                enteredAt,
                daysSinceLastParticipation,
                null,
                null,
                context.cheerCountByUserId().getOrDefault(userId, 0)
        );
    }

    private MemberTimerSnapshot buildSnapshotFromActiveSession(
            UUID userId,
            FocusSession focusSession,
            LocalDateTime now,
            MemberLoadContext context
    ) {
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
                FocusInterval currentInterval = context.latestIntervalBySessionId().get(focusSession.getId());
                if (currentInterval != null) {
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
            Todo todo = context.todoById().get(focusSession.getTodoId());
            if (todo != null) {
                todoTitle = todo.getTask();
            }
        }

        LocalDateTime lastActiveAt = focusSession.getEndedAt() != null
                ? focusSession.getEndedAt()
                : focusSession.getStartedAt();
        LocalDateTime enteredAt = context.enteredAtByUserId().get(userId);
        if (enteredAt == null) {
            enteredAt = focusSession.getStartedAt();
        }
        Long daysSinceLastParticipation = enteredAt != null ? Duration.between(enteredAt, now).toDays() : null;

        return new MemberTimerSnapshot(
                participationStatus,
                lastActiveAt,
                enteredAt,
                daysSinceLastParticipation,
                personalTimerSeconds,
                todoTitle,
                context.cheerCountByUserId().getOrDefault(userId, 0)
        );
    }

    private MemberLoadContext loadMemberLoadContext(List<UUID> memberIds) {
        Map<UUID, LocalDateTime> enteredAtByUserId = officialLoungePresenceService.getEnteredAtMap(memberIds);
        Map<UUID, Integer> cheerCountByUserId = officialLoungePresenceService.getCheerCountMap(memberIds);
        Map<UUID, UserCharacter> topUserCharacterByUserId = loadTopUserCharacterByUserId(memberIds);
        Map<UUID, ActiveFocusSession> activeSessionByUserId = loadActiveSessionByUserId(memberIds);
        Map<UUID, FocusSession> latestSessionByUserId = loadLatestSessionByUserId(memberIds);
        Map<UUID, FocusInterval> latestIntervalBySessionId = loadLatestIntervalBySessionId(
                activeSessionByUserId.values().stream()
                        .map(ActiveFocusSession::getSessionId)
                        .distinct()
                        .toList()
        );
        Map<UUID, Todo> todoById = loadTodosById(
                latestSessionByUserId.values().stream()
                        .map(FocusSession::getTodoId)
                        .filter(todoId -> todoId != null)
                        .distinct()
                        .toList()
        );
        ImageCharacter defaultImageCharacter = imageCharacterRepository.findFirstByLevelAndIsActiveTrueOrderByCreatedAtAsc(1)
                .orElse(null);

        return new MemberLoadContext(
                topUserCharacterByUserId,
                Set.copyOf(activeSessionByUserId.keySet()),
                latestSessionByUserId,
                latestIntervalBySessionId,
                todoById,
                enteredAtByUserId,
                cheerCountByUserId,
                defaultImageCharacter
        );
    }

    private Map<UUID, UserCharacter> loadTopUserCharacterByUserId(List<UUID> memberIds) {
        if (memberIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, UserCharacter> topUserCharacterByUserId = new HashMap<>();
        userCharacterRepository.findAllByUserIdInOrderByUserIdAscImageCharacter_LevelDescImageCharacter_CreatedAtAsc(memberIds)
                .forEach(userCharacter -> topUserCharacterByUserId.putIfAbsent(userCharacter.getUser().getId(), userCharacter));
        return topUserCharacterByUserId;
    }

    private Map<UUID, ActiveFocusSession> loadActiveSessionByUserId(List<UUID> memberIds) {
        if (memberIds.isEmpty()) {
            return Map.of();
        }

        return activeFocusSessionRepository.findAllByUserIdIn(memberIds).stream()
                .collect(Collectors.toMap(
                        ActiveFocusSession::getUserId,
                        activeSession -> activeSession,
                        (left, right) -> left
                ));
    }

    private Map<UUID, FocusSession> loadLatestSessionByUserId(List<UUID> memberIds) {
        if (memberIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, FocusSession> latestSessionByUserId = new HashMap<>();
        focusSessionRepository.findAllByUserIdInOrderByUserIdAscStartedAtDesc(memberIds)
                .forEach(session -> latestSessionByUserId.putIfAbsent(session.getUserId(), session));
        return latestSessionByUserId;
    }

    private Map<UUID, FocusInterval> loadLatestIntervalBySessionId(List<UUID> sessionIds) {
        if (sessionIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, FocusInterval> latestIntervalBySessionId = new HashMap<>();
        focusIntervalRepository.findAllBySessionIdInOrderBySessionIdAscStartedAtDesc(sessionIds)
                .forEach(interval -> latestIntervalBySessionId.putIfAbsent(interval.getSessionId(), interval));
        return latestIntervalBySessionId;
    }

    private Map<UUID, Todo> loadTodosById(List<UUID> todoIds) {
        if (todoIds.isEmpty()) {
            return Map.of();
        }

        return todoRepository.findAllById(todoIds).stream()
                .collect(Collectors.toMap(Todo::getId, todo -> todo, (left, right) -> left));
    }

    private record MemberTimerSnapshot(
            GroupParticipationStatus participationStatus,
            LocalDateTime lastActiveAt,
            LocalDateTime enteredAt,
            Long daysSinceLastParticipation,
            Long personalTimerSeconds,
            String todoTitle,
            Integer cheerCount
    ) {
    }

    private Integer getLevelFromUser(User user, Map<UUID, UserCharacter> topUserCharacterByUserId) {
        return Optional.ofNullable(topUserCharacterByUserId.get(user.getId()))
                .map(uc -> uc.getImageCharacter().getLevel())
                .orElse(1);
    }

    private String getProfileUrlFromUser(
            User user,
            Map<UUID, UserCharacter> topUserCharacterByUserId,
            ImageCharacter defaultImageCharacter
    ) {
        if (user.getImageUrl() != null) {
            return user.getImageUrl();
        }

        UserCharacter userCharacter = topUserCharacterByUserId.get(user.getId());
        if (userCharacter != null) {
            return userCharacter.getImageCharacter().getImageUrl();
        }

        return defaultImageCharacter != null ? defaultImageCharacter.getImageUrl() : null;
    }

    private record MemberLoadContext(
            Map<UUID, UserCharacter> topUserCharacterByUserId,
            Set<UUID> activeSessionUserIds,
            Map<UUID, FocusSession> latestSessionByUserId,
            Map<UUID, FocusInterval> latestIntervalBySessionId,
            Map<UUID, Todo> todoById,
            Map<UUID, LocalDateTime> enteredAtByUserId,
            Map<UUID, Integer> cheerCountByUserId,
            ImageCharacter defaultImageCharacter
    ) {
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
