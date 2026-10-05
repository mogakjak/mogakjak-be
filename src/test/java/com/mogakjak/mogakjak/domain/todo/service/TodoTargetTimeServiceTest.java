package com.mogakjak.mogakjak.domain.todo.service;

import com.mogakjak.mogakjak.domain.timer.entity.ActiveFocusSession;
import com.mogakjak.mogakjak.domain.timer.entity.FocusSession;
import com.mogakjak.mogakjak.domain.timer.enumerate.ParticipationType;
import com.mogakjak.mogakjak.domain.timer.enumerate.TimerMode;
import com.mogakjak.mogakjak.domain.timer.enumerate.TimerStatus;
import com.mogakjak.mogakjak.domain.timer.repository.ActiveFocusSessionRepository;
import com.mogakjak.mogakjak.domain.timer.repository.FocusSessionRepository;
import com.mogakjak.mogakjak.domain.todo.controller.dto.TodoResponse;
import com.mogakjak.mogakjak.domain.todo.controller.dto.UpdateTodoRequest;
import com.mogakjak.mogakjak.domain.todo.controller.dto.UpdateTodoTargetTimeRequest;
import com.mogakjak.mogakjak.domain.todo.entity.Todo;
import com.mogakjak.mogakjak.domain.todo.repository.TodoRepository;
import com.mogakjak.mogakjak.domain.user.entity.Category;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.repository.CategoryRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserRepository;
import com.mogakjak.mogakjak.global.exception.CustomException;
import com.mogakjak.mogakjak.global.exception.status.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TodoTargetTimeServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private TodoRepository todoRepository;
    @Mock private ActiveFocusSessionRepository activeFocusSessionRepository;
    @Mock private FocusSessionRepository focusSessionRepository;
    @Mock private TodoLastWorkedAtService todoLastWorkedAtService;
    @InjectMocks private TodoServiceImpl service;

    private final UUID userId = UUID.randomUUID();
    private final UUID todoId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();
    private final LocalDate date = LocalDate.of(2026, 10, 5);
    private User user;
    private Todo todo;

    @BeforeEach
    void setUp() {
        user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", userId);
        Category category = Category.builder().user(user).build();
        ReflectionTestUtils.setField(category, "id", categoryId);
        todo = Todo.builder().user(user).category(category).task("독서").date(date)
                .targetTimeInSeconds(3600).actualTimeInSeconds(900).isCompleted(true).build();
        ReflectionTestUtils.setField(todo, "id", todoId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    }

    @Test
    void setChangeAndClearAreReturnedByExistingListWithoutChangingOtherFields() {
        allowTodoAccess();
        when(todoRepository.findAllByUserAndIsDeletedFalseOrderByCreatedAtDesc(user)).thenReturn(List.of(todo));

        service.updateTodoTargetTime(userId, todoId, request(3600));
        assertEquals(3600, service.getUserTodos(user).getFirst().getTargetTimeInSeconds());
        TodoResponse changed = service.updateTodoTargetTime(userId, todoId, request(7200));
        assertEquals(7200, service.getUserTodos(user).getFirst().getTargetTimeInSeconds());
        assertEquals(12, changed.getProgressRate());
        service.updateTodoTargetTime(userId, todoId, request(null));
        TodoResponse cleared = service.getUserTodos(user).getFirst();
        assertNull(cleared.getTargetTimeInSeconds());
        assertNull(cleared.getProgressRate());
        assertEquals("독서", cleared.getTask());
        assertEquals(categoryId, cleared.getCategoryId());
        assertEquals(date, cleared.getDate());
        assertEquals(900, cleared.getActualTimeInSeconds());
        assertTrue(cleared.getIsCompleted());
        verifyNoInteractions(focusSessionRepository);
    }

    static Stream<Arguments> activeModes() {
        return Arrays.stream(TimerMode.values()).flatMap(mode -> Stream.of(TimerStatus.RUNNING, TimerStatus.PAUSED)
                .map(status -> Arguments.of(mode, status)));
    }

    @ParameterizedTest
    @MethodSource("activeModes")
    void updatesLinkedActiveSessionAcrossModesAndStates(TimerMode mode, TimerStatus status) {
        allowTodoAccess();
        FocusSession session = session(mode, ParticipationType.GROUP);
        if (status == TimerStatus.PAUSED) session.pause(25);
        linkActiveSession(session);

        TodoResponse response = service.updateTodoTargetTime(userId, todoId, request(7200));

        assertEquals(12, response.getProgressRate());
        verify(focusSessionRepository).updateActiveTodoProgressRate(session.getId(), userId, todoId,
                response.getProgressRate(), List.of(TimerStatus.RUNNING, TimerStatus.PAUSED));
        assertEquals(status, session.getStatus());
        assertEquals(mode == TimerMode.TIMER ? 1800L : null, session.getTargetDuration());
        assertEquals(mode == TimerMode.POMODORO ? 60L : null, session.getFocusDuration());
        assertEquals(900, todo.getActualTimeInSeconds());
        verify(focusSessionRepository, never()).save(any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {1200})
    void clearOrSetUpdatesPausedPersonalSessionImmediately(Integer target) {
        allowTodoAccess();
        FocusSession session = session(TimerMode.TIMER, ParticipationType.INDIVIDUAL);
        session.pause(25);
        linkActiveSession(session);
        TodoResponse response = service.updateTodoTargetTime(userId, todoId, request(target));
        assertEquals(target == null ? null : 75, response.getProgressRate());
        verify(focusSessionRepository).updateActiveTodoProgressRate(session.getId(), userId, todoId,
                response.getProgressRate(), List.of(TimerStatus.RUNNING, TimerStatus.PAUSED));
    }

    @Test
    void doesNotUpdateSessionForAnotherTodo() {
        allowTodoAccess();
        FocusSession session = session(TimerMode.TIMER, ParticipationType.INDIVIDUAL);
        ReflectionTestUtils.setField(session, "todoId", UUID.randomUUID());
        linkActiveSession(session);
        service.updateTodoTargetTime(userId, todoId, request(7200));
        assertEquals(25, session.getProgressRate());
        verify(focusSessionRepository, never()).save(any());
        verify(focusSessionRepository, never()).updateActiveTodoProgressRate(any(), any(), any(), any(), any());
    }

    @Test
    void doesNotRewriteFinishedSessionProgress() {
        allowTodoAccess();
        FocusSession session = session(TimerMode.TIMER, ParticipationType.INDIVIDUAL);
        session.end(LocalDateTime.now(), 25);
        linkActiveSession(session);
        service.updateTodoTargetTime(userId, todoId, request(null));
        assertEquals(25, session.getProgressRate());
        verify(focusSessionRepository, never()).save(any());
        verify(focusSessionRepository, never()).updateActiveTodoProgressRate(any(), any(), any(), any(), any());
    }

    @Test
    void doesNotUpdateAnotherUsersSession() {
        allowTodoAccess();
        FocusSession session = session(TimerMode.TIMER, ParticipationType.INDIVIDUAL);
        ReflectionTestUtils.setField(session, "userId", UUID.randomUUID());
        linkActiveSession(session);
        service.updateTodoTargetTime(userId, todoId, request(7200));
        assertEquals(25, session.getProgressRate());
        verify(focusSessionRepository, never()).save(any());
        verify(focusSessionRepository, never()).updateActiveTodoProgressRate(any(), any(), any(), any(), any());
    }

    @Test
    void forbiddenOrDeletedTodoIsNotMutated() {
        // The repository's owner + isDeleted=false predicate must succeed before mutation.
        CustomException exception = assertThrows(CustomException.class,
                () -> service.updateTodoTargetTime(userId, todoId, request(null)));
        assertEquals(ErrorCode.FORBIDDEN_TODO_ACCESS, exception.getStatusCode());
        verify(todoRepository).findByIdAndUserAndIsDeletedFalse(todoId, user);
        verifyNoInteractions(activeFocusSessionRepository, focusSessionRepository);
    }

    @Test
    void fullPutUsesSameSessionProgressSynchronization() {
        allowTodoAccess();
        FocusSession session = session(TimerMode.TIMER, ParticipationType.INDIVIDUAL);
        linkActiveSession(session);
        service.updateTodo(userId, todoId, new UpdateTodoRequest(categoryId, "독서", date, 7200));
        verify(focusSessionRepository).updateActiveTodoProgressRate(session.getId(), userId, todoId,
                12, List.of(TimerStatus.RUNNING, TimerStatus.PAUSED));
        assertEquals(900, todo.getActualTimeInSeconds());
    }

    private UpdateTodoTargetTimeRequest request(Integer target) {
        UpdateTodoTargetTimeRequest request = new UpdateTodoTargetTimeRequest();
        request.setTargetTimeInSeconds(target);
        return request;
    }

    private void allowTodoAccess() {
        when(todoRepository.findByIdAndUserAndIsDeletedFalse(todoId, user)).thenReturn(Optional.of(todo));
    }

    private FocusSession session(TimerMode mode, ParticipationType type) {
        LocalDateTime now = LocalDateTime.now();
        UUID groupId = type == ParticipationType.GROUP ? UUID.randomUUID() : null;
        FocusSession session = switch (mode) {
            case TIMER -> FocusSession.createTimerSession(userId, todo, now, 1800L, type, groupId, true, true);
            case STOPWATCH -> FocusSession.createStopwatchSession(userId, todo, now, type, groupId, true, true);
            case POMODORO -> FocusSession.createPomodoroSession(userId, todo, now, 60L, 30L, 2, type, groupId, true, true);
        };
        ReflectionTestUtils.setField(session, "id", UUID.randomUUID());
        session.setProgressRate(25);
        return session;
    }

    private void linkActiveSession(FocusSession session) {
        when(activeFocusSessionRepository.findByUserId(userId)).thenReturn(Optional.of(
                ActiveFocusSession.create(session.getId(), userId, LocalDateTime.now())));
        when(focusSessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
    }
}
