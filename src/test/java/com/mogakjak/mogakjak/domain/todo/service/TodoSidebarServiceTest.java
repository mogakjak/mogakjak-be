package com.mogakjak.mogakjak.domain.todo.service;

import com.mogakjak.mogakjak.domain.timer.entity.*;
import com.mogakjak.mogakjak.domain.timer.enumerate.*;
import com.mogakjak.mogakjak.domain.timer.repository.*;
import com.mogakjak.mogakjak.domain.todo.entity.Todo;
import com.mogakjak.mogakjak.domain.todo.repository.TodoRepository;
import com.mogakjak.mogakjak.domain.user.entity.*;
import com.mogakjak.mogakjak.domain.user.repository.UserRepository;
import com.mogakjak.mogakjak.global.exception.CustomException;
import com.mogakjak.mogakjak.global.exception.status.ErrorCode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TodoSidebarServiceTest {
    @Mock private UserRepository users;
    @Mock private TodoRepository todos;
    @Mock private ActiveFocusSessionRepository active;
    @Mock private FocusSessionRepository sessions;
    @Mock private FocusIntervalRepository intervals;
    @Mock private TodoLastWorkedAtService lastWorked;
    @InjectMocks private TodoSidebarService service;
    private final UUID userId = UUID.randomUUID(), todoId = UUID.randomUUID(), sessionId = UUID.randomUUID();
    private User user;
    private Todo todo;

    @BeforeEach
    void setup() {
        user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", userId);
        Category category = Category.builder().user(user).name("기본").build();
        ReflectionTestUtils.setField(category, "id", UUID.randomUUID());
        todo = Todo.builder().user(user).category(category).task("독서").targetTimeInSeconds(3600)
                .actualTimeInSeconds(900).build();
        ReflectionTestUtils.setField(todo, "id", todoId);
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(todos.findByIdAndUserAndIsDeletedFalse(todoId, user)).thenReturn(Optional.of(todo));
    }

    @Test
    void noSessionReturnsStoredGoalCategoryHistoryAndPublicDefaults() {
        LocalDateTime end = LocalDateTime.now().minusDays(2);
        when(lastWorked.getLastWorkedAt(eq(userId), eq(List.of(todoId)), any())).thenReturn(Map.of(todoId, end));
        var response = service.getTodoDetail(userId, todoId);
        assertEquals(todoId, response.todo().getId());
        assertEquals("기본", response.category().getName());
        assertEquals(3600, response.todo().getTargetTimeInSeconds());
        assertEquals(900L, response.accumulatedTimeInSeconds());
        assertEquals(25, response.progressRate());
        assertTrue(response.isTaskPublic());
        assertTrue(response.isTimerPublic());
        assertNull(response.activeSession());
        assertEquals(end, response.todo().getLastWorkedAt().atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime());
        assertEquals(ZoneOffset.ofHours(9), response.serverTime().getOffset());
        verifyNoInteractions(intervals, sessions);
    }

    @Test
    void unsetGoalHasNullSavedAndLiveProgress() {
        todo.updateTargetTime(null);
        var response = service.getTodoDetail(userId, todoId);
        assertNull(response.todo().getTargetTimeInSeconds());
        assertNull(response.todo().getProgressRate());
        assertNull(response.progressRate());
    }

    static Stream<Arguments> modesAndParticipation() {
        return Arrays.stream(TimerMode.values()).flatMap(mode -> Arrays.stream(ParticipationType.values())
                .map(type -> Arguments.of(mode, type)));
    }

    @ParameterizedTest
    @MethodSource("modesAndParticipation")
    void runningFocusReturnsLiveTimeWithoutMutatingSavedCounters(TimerMode mode, ParticipationType type) {
        FocusSession session = link(mode, type);
        LocalDateTime start = LocalDateTime.now().minusSeconds(60);
        FocusInterval current = FocusInterval.create(sessionId, start,
                mode == TimerMode.POMODORO ? PomodoroPhaseType.FOCUS : PomodoroPhaseType.NORMAL, 1);
        when(intervals.findAllBySessionId(sessionId)).thenReturn(List.of(current));
        var response = service.getTodoDetail(userId, todoId);
        long elapsed = Duration.between(start,
                response.serverTime().atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()).getSeconds();
        assertEquals(900L + elapsed, response.accumulatedTimeInSeconds());
        assertEquals(elapsed, response.activeSession().focusTimeInSeconds());
        assertEquals(elapsed, response.activeSession().phaseElapsedTimeInSeconds());
        assertTrue(response.activeSession().isCounting());
        assertEquals(mode, response.activeSession().mode());
        assertEquals(type, response.activeSession().participationType());
        assertEquals(mode == TimerMode.TIMER ? 1800L : null, response.activeSession().targetDuration());
        assertEquals((int) ((900 + elapsed) * 100 / 3600), response.progressRate());
        assertEquals(900, todo.getActualTimeInSeconds());
        assertEquals(25, response.todo().getProgressRate());
        verify(todos, never()).save(any());
        verify(sessions, never()).save(any());
        verify(intervals, never()).save(any());
    }

    @Test
    void pauseBreakAndVisibilityAreReportedWithoutAddingRestTime() {
        FocusSession session = link(TimerMode.POMODORO, ParticipationType.GROUP);
        session.updateTaskVisibility(false);
        session.updateTimerVisibility(false);
        LocalDateTime now = LocalDateTime.now();
        FocusInterval focus = FocusInterval.create(sessionId, now.minusSeconds(100), PomodoroPhaseType.FOCUS, 1);
        focus.end(now.minusSeconds(40));
        FocusInterval rest = FocusInterval.create(sessionId, now.minusSeconds(30), PomodoroPhaseType.BREAK, 1);
        rest.end(now.minusSeconds(10));
        session.pause(25);
        when(intervals.findAllBySessionId(sessionId)).thenReturn(List.of(rest, focus));
        var response = service.getTodoDetail(userId, todoId);
        assertEquals(900L, response.accumulatedTimeInSeconds());
        assertEquals(60L, response.activeSession().focusTimeInSeconds());
        assertEquals(20L, response.activeSession().phaseElapsedTimeInSeconds());
        assertEquals(PomodoroPhaseType.BREAK, response.activeSession().phaseType());
        assertFalse(response.activeSession().isCounting());
        assertFalse(response.isTaskPublic());
        assertFalse(response.isTimerPublic());
        session.updateTaskVisibility(true);
        assertTrue(service.getTodoDetail(userId, todoId).isTaskPublic());
    }

    @ParameterizedTest
    @ValueSource(strings = {"owner", "todo", "finished", "missing"})
    void invalidOrUnrelatedActiveSessionIsNotExposed(String invalid) {
        FocusSession session = link(TimerMode.TIMER, ParticipationType.INDIVIDUAL);
        switch (invalid) {
            case "owner" -> ReflectionTestUtils.setField(session, "userId", UUID.randomUUID());
            case "todo" -> ReflectionTestUtils.setField(session, "todoId", UUID.randomUUID());
            case "finished" -> session.end(LocalDateTime.now(), 25);
            case "missing" -> when(sessions.findById(sessionId)).thenReturn(Optional.empty());
        }
        var response = service.getTodoDetail(userId, todoId);
        assertNull(response.activeSession());
        assertEquals(900L, response.accumulatedTimeInSeconds());
        assertTrue(response.isTaskPublic());
        verifyNoInteractions(intervals);
    }

    @Test
    void missingDeletedOrOtherUsersTodoIsRejectedBeforeSessionLookup() {
        when(todos.findByIdAndUserAndIsDeletedFalse(todoId, user)).thenReturn(Optional.empty());
        var error = assertThrows(CustomException.class, () -> service.getTodoDetail(userId, todoId));
        assertEquals(ErrorCode.FORBIDDEN_TODO_ACCESS, error.getStatusCode());
        verifyNoInteractions(active, sessions, intervals, lastWorked);
    }

    @Test
    void deletedCategoryDoesNotLeakDetails() {
        todo.getCategory().softDelete();
        assertThrows(CustomException.class, () -> service.getTodoDetail(userId, todoId));
        verifyNoInteractions(active, sessions, intervals, lastWorked);
    }

    private FocusSession link(TimerMode mode, ParticipationType type) {
        LocalDateTime now = LocalDateTime.now().minusMinutes(10);
        UUID groupId = type == ParticipationType.GROUP ? UUID.randomUUID() : null;
        FocusSession session = switch (mode) {
            case TIMER -> FocusSession.createTimerSession(userId, todo, now, 1800L, type, groupId, true, true);
            case STOPWATCH -> FocusSession.createStopwatchSession(userId, todo, now, type, groupId, true, true);
            case POMODORO -> FocusSession.createPomodoroSession(userId, todo, now, 60L, 30L, 2, type, groupId, true, true);
        };
        ReflectionTestUtils.setField(session, "id", sessionId);
        when(active.findByUserId(userId)).thenReturn(Optional.of(ActiveFocusSession.create(sessionId, userId, now)));
        when(sessions.findById(sessionId)).thenReturn(Optional.of(session));
        return session;
    }
}
