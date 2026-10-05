package com.mogakjak.mogakjak.domain.timer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mogakjak.mogakjak.domain.group.repository.GroupRepository;
import com.mogakjak.mogakjak.domain.group.service.GroupService;
import com.mogakjak.mogakjak.domain.lounge.service.OfficialLoungeService;
import com.mogakjak.mogakjak.domain.timer.dto.request.PomodoroStartRequest;
import com.mogakjak.mogakjak.domain.timer.dto.request.StopwatchStartRequest;
import com.mogakjak.mogakjak.domain.timer.dto.request.TimerStartRequest;
import com.mogakjak.mogakjak.domain.timer.dto.response.TimerResponse;
import com.mogakjak.mogakjak.domain.timer.entity.ActiveFocusSession;
import com.mogakjak.mogakjak.domain.timer.entity.FocusInterval;
import com.mogakjak.mogakjak.domain.timer.entity.FocusSession;
import com.mogakjak.mogakjak.domain.timer.enumerate.ParticipationType;
import com.mogakjak.mogakjak.domain.timer.enumerate.PomodoroPhaseType;
import com.mogakjak.mogakjak.domain.timer.enumerate.TimerMode;
import com.mogakjak.mogakjak.domain.timer.enumerate.TimerStatus;
import com.mogakjak.mogakjak.domain.timer.repository.ActiveFocusSessionRepository;
import com.mogakjak.mogakjak.domain.timer.repository.FocusIntervalRepository;
import com.mogakjak.mogakjak.domain.timer.repository.FocusSessionRepository;
import com.mogakjak.mogakjak.domain.todo.entity.Todo;
import com.mogakjak.mogakjak.domain.todo.repository.TodoRepository;
import com.mogakjak.mogakjak.domain.user.entity.Category;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.repository.UserGroupRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserRepository;
import com.mogakjak.mogakjak.global.exception.CustomException;
import com.mogakjak.mogakjak.global.exception.status.ErrorCode;
import com.mogakjak.mogakjak.global.websocket.service.GroupMemberStatusService;
import com.mogakjak.mogakjak.global.websocket.service.TimerCompletionNotificationService;
import com.mogakjak.mogakjak.global.websocket.service.UserActiveStatusService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FocusSessionOptionalTargetTest {

    @Mock private FocusSessionRepository focusSessionRepository;
    @Mock private FocusIntervalRepository focusIntervalRepository;
    @Mock private ActiveFocusSessionRepository activeFocusSessionRepository;
    @Mock private TodoRepository todoRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserGroupRepository userGroupRepository;
    @Mock private GroupRepository groupRepository;
    @Mock private GroupMemberStatusService groupMemberStatusService;
    @Mock private TimerCompletionNotificationService timerCompletionNotificationService;
    @Mock private OfficialLoungeService officialLoungeService;
    @Mock private UserActiveStatusService userActiveStatusService;
    @Mock private GroupService groupService;
    @InjectMocks private FocusSessionServiceImpl service;

    private final UUID userId = UUID.randomUUID();
    private final UUID todoId = UUID.randomUUID();
    private final UUID groupId = UUID.randomUUID();
    private final List<FocusInterval> intervals = new ArrayList<>();
    private User user;
    private Todo todo;
    private FocusSession session;
    private ActiveFocusSession active;

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
        user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", userId);
        Category category = Category.builder().user(user).build();
        ReflectionTestUtils.setField(category, "id", UUID.randomUUID());
        todo = Todo.builder().user(user).category(category).task("독서").date(LocalDate.now())
                .actualTimeInSeconds(600).build();
        ReflectionTestUtils.setField(todo, "id", todoId);
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    static Stream<Arguments> modesAndParticipationTypes() {
        return Arrays.stream(TimerMode.values()).flatMap(mode -> Arrays.stream(ParticipationType.values())
                .map(type -> Arguments.of(mode, type)));
    }

    @ParameterizedTest
    @MethodSource("modesAndParticipationTypes")
    void unsetTargetSupportsFullLifecycleAndAccumulatesTime(TimerMode mode, ParticipationType type) {
        prepareStart();
        TimerResponse started = start(mode, type);
        assertNull(started.progressRate());
        assertNull(started.todo().getTargetTimeInSeconds());
        assertEquals(mode == TimerMode.TIMER ? 1800L : null, started.targetDuration());
        JsonNode json = new ObjectMapper().findAndRegisterModules().valueToTree(started);
        assertTrue(json.has("progressRate"));
        assertTrue(json.get("progressRate").isNull());

        prepareLifecycle();
        moveCurrentIntervalBack(60);
        TimerResponse paused = service.pauseSession(user, session.getId());
        assertEquals(TimerStatus.PAUSED, paused.status());
        assertNull(paused.progressRate());
        assertTrue(todo.getActualTimeInSeconds() >= 660);
        int timeAfterPause = todo.getActualTimeInSeconds();

        TimerResponse resumed = service.resumeSession(user, session.getId());
        assertEquals(TimerStatus.RUNNING, resumed.status());
        assertNull(resumed.progressRate());
        assertEquals(timeAfterPause, todo.getActualTimeInSeconds());

        moveCurrentIntervalBack(30);
        TimerResponse finished = service.finishSession(user, session.getId());
        assertEquals(TimerStatus.FINISHED, finished.status());
        assertNull(finished.progressRate());
        assertTrue(todo.getActualTimeInSeconds() >= timeAfterPause + 30);
        assertEquals(todo.getActualTimeInSeconds() - 600L, finished.totalDuration());
        assertNull(active);
        verify(timerCompletionNotificationService).scheduleCompletionNotification(session.getId());
        verify(timerCompletionNotificationService, times(2)).rescheduleCompletionNotification(session.getId());
        verify(timerCompletionNotificationService).cancelScheduledNotification(session.getId());
    }

    @Test
    void unsetTargetSupportsPomodoroPhaseChangesAndCompletion() {
        prepareStart();
        start(TimerMode.POMODORO, ParticipationType.INDIVIDUAL);
        prepareLifecycle();
        when(focusIntervalRepository.findAllBySessionId(session.getId())).thenReturn(intervals);

        moveCurrentIntervalBack(61);
        TimerResponse breakPhase = service.nextPomodoroPhase(user, session.getId());
        assertEquals(PomodoroPhaseType.BREAK, breakPhase.pomodoroInfo().phaseType());
        assertNull(breakPhase.progressRate());
        int firstFocusTime = todo.getActualTimeInSeconds();
        assertEquals(600 + focusSeconds(), firstFocusTime);

        moveCurrentIntervalBack(31);
        TimerResponse focusPhase = service.nextPomodoroPhase(user, session.getId());
        assertEquals(PomodoroPhaseType.FOCUS, focusPhase.pomodoroInfo().phaseType());
        assertNull(focusPhase.progressRate());
        assertEquals(firstFocusTime, todo.getActualTimeInSeconds());

        moveCurrentIntervalBack(61);
        TimerResponse finished = service.nextPomodoroPhase(user, session.getId());
        assertEquals(TimerStatus.FINISHED, finished.status());
        assertNull(finished.progressRate());
        assertEquals(600 + focusSeconds(), todo.getActualTimeInSeconds().longValue());
        assertNull(active);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void pomodoroBreakPauseOrFinishNeverAddsRestToTodo(boolean pauseFirst) {
        prepareStart();
        start(TimerMode.POMODORO, ParticipationType.INDIVIDUAL);
        prepareLifecycle();
        when(focusIntervalRepository.findAllBySessionId(session.getId())).thenReturn(intervals);
        moveCurrentIntervalBack(61);
        service.nextPomodoroPhase(user, session.getId());
        int afterFocus = todo.getActualTimeInSeconds();
        moveCurrentIntervalBack(20);
        if (pauseFirst) service.pauseSession(user, session.getId());
        service.finishSession(user, session.getId());
        assertEquals(afterFocus, todo.getActualTimeInSeconds());
    }

    @Test
    void finalPausedFocusIsNotCreditedTwiceWhenCompletingPomodoro() {
        prepareStart();
        start(TimerMode.POMODORO, ParticipationType.INDIVIDUAL);
        ReflectionTestUtils.setField(session, "repeatCount", 1);
        prepareLifecycle();
        when(focusIntervalRepository.findAllBySessionId(session.getId())).thenReturn(intervals);
        moveCurrentIntervalBack(61);
        service.pauseSession(user, session.getId());
        int afterPause = todo.getActualTimeInSeconds();
        service.nextPomodoroPhase(user, session.getId());
        assertEquals(afterPause, todo.getActualTimeInSeconds());
        assertEquals(600 + focusSeconds(), todo.getActualTimeInSeconds().longValue());
        assertEquals(TimerStatus.FINISHED, session.getStatus());
    }

    @Test
    void resumedFocusAddsOnlyUncreditedSegmentWhenSwitchingToBreak() {
        prepareStart();
        start(TimerMode.POMODORO, ParticipationType.INDIVIDUAL);
        prepareLifecycle();
        when(focusIntervalRepository.findAllBySessionId(session.getId())).thenReturn(intervals);
        moveCurrentIntervalBack(30);
        service.pauseSession(user, session.getId());
        service.resumeSession(user, session.getId());
        moveCurrentIntervalBack(31);
        service.nextPomodoroPhase(user, session.getId());
        assertEquals(600 + focusSeconds(), todo.getActualTimeInSeconds().longValue());
        int beforeFinish = todo.getActualTimeInSeconds();
        service.finishSession(user, session.getId());
        assertEquals(beforeFinish, todo.getActualTimeInSeconds());
    }

    private long focusSeconds() {
        return intervals.stream().filter(interval -> interval.getPhaseType() == PomodoroPhaseType.FOCUS)
                .filter(interval -> interval.getEndedAt() != null)
                .mapToLong(interval -> java.time.Duration.between(interval.getStartedAt(), interval.getEndedAt()).getSeconds())
                .sum();
    }

    @Test
    void existingTargetStillCalculatesProgressFromTodoAccumulatedTime() {
        todo.updateInfo(todo.getCategory(), todo.getTask(), todo.getDate(), 1200);
        prepareStart();
        TimerResponse started = start(TimerMode.TIMER, ParticipationType.INDIVIDUAL);
        assertEquals(50, started.progressRate());
        assertEquals(1200, started.todo().getTargetTimeInSeconds());

        prepareLifecycle();
        moveCurrentIntervalBack(60);
        TimerResponse paused = service.pauseSession(user, session.getId());
        assertEquals((int) Math.floor(todo.getActualTimeInSeconds() / 1200.0 * 100), paused.progressRate());

        int timeAfterPause = todo.getActualTimeInSeconds();
        TimerResponse finished = service.finishSession(user, session.getId());
        assertEquals(paused.progressRate(), finished.progressRate());
        assertEquals(timeAfterPause, todo.getActualTimeInSeconds());
    }

    @ParameterizedTest
    @EnumSource(TimerMode.class)
    void missingTodoCannotStart(TimerMode mode) {
        CustomException exception = assertThrows(CustomException.class,
                () -> start(mode, ParticipationType.INDIVIDUAL));
        assertEquals(ErrorCode.TODO_NOT_FOUND, exception.getStatusCode());
        verify(focusSessionRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(TimerMode.class)
    void deletedTodoCannotStart(TimerMode mode) {
        todo.softDelete();
        when(todoRepository.findById(todoId)).thenReturn(Optional.of(todo));
        CustomException exception = assertThrows(CustomException.class,
                () -> start(mode, ParticipationType.INDIVIDUAL));
        assertEquals(ErrorCode.TODO_DELETED, exception.getStatusCode());
        verify(focusSessionRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(TimerMode.class)
    void otherUsersTodoCannotStart(TimerMode mode) {
        ReflectionTestUtils.setField(todo.getCategory().getUser(), "id", UUID.randomUUID());
        User caller = User.builder().build();
        ReflectionTestUtils.setField(caller, "id", userId);
        user = caller;
        when(todoRepository.findById(todoId)).thenReturn(Optional.of(todo));
        CustomException exception = assertThrows(CustomException.class,
                () -> start(mode, ParticipationType.INDIVIDUAL));
        assertEquals(ErrorCode.FORBIDDEN_TODO_ACCESS, exception.getStatusCode());
        verify(focusSessionRepository, never()).save(any());
    }

    private TimerResponse start(TimerMode mode, ParticipationType type) {
        UUID selectedGroupId = type == ParticipationType.GROUP ? groupId : null;
        return switch (mode) {
            case TIMER -> service.startTimer(user, TimerStartRequest.builder().todoId(todoId)
                    .targetSeconds(1800L).participationType(type).groupId(selectedGroupId).build());
            case STOPWATCH -> service.startStopwatch(user,
                    new StopwatchStartRequest(todoId, type, selectedGroupId, null, null));
            case POMODORO -> service.startPomodoro(user,
                    new PomodoroStartRequest(todoId, 60L, 30L, 2, type, selectedGroupId, null, null));
        };
    }

    private void prepareStart() {
        when(todoRepository.findById(todoId)).thenReturn(Optional.of(todo));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(activeFocusSessionRepository.findByUserId(userId)).thenAnswer(invocation -> Optional.ofNullable(active));
        when(focusSessionRepository.save(any(FocusSession.class))).thenAnswer(invocation -> {
            session = invocation.getArgument(0);
            if (session.getId() == null) ReflectionTestUtils.setField(session, "id", UUID.randomUUID());
            return session;
        });
        when(activeFocusSessionRepository.save(any(ActiveFocusSession.class))).thenAnswer(invocation -> {
            active = invocation.getArgument(0);
            ReflectionTestUtils.setField(active, "id", UUID.randomUUID());
            return active;
        });
        when(focusIntervalRepository.save(any(FocusInterval.class))).thenAnswer(invocation -> {
            FocusInterval interval = invocation.getArgument(0);
            intervals.add(interval);
            return interval;
        });
    }

    private void prepareLifecycle() {
        when(focusSessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(focusIntervalRepository.findTopBySessionIdOrderByStartedAtDesc(session.getId()))
                .thenAnswer(invocation -> Optional.of(intervals.get(intervals.size() - 1)));
        doAnswer(invocation -> { active = null; return null; }).when(activeFocusSessionRepository).deleteById(any());
    }

    private void moveCurrentIntervalBack(long seconds) {
        ReflectionTestUtils.setField(intervals.get(intervals.size() - 1), "startedAt",
                LocalDateTime.now().minusSeconds(seconds));
    }
}
