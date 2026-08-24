package com.mogakjak.mogakjak.domain.timer.service;

import com.mogakjak.mogakjak.domain.group.repository.GroupRepository;
import com.mogakjak.mogakjak.domain.group.service.GroupService;
import com.mogakjak.mogakjak.domain.timer.entity.ActiveFocusSession;
import com.mogakjak.mogakjak.domain.timer.entity.FocusInterval;
import com.mogakjak.mogakjak.domain.timer.entity.FocusSession;
import com.mogakjak.mogakjak.domain.timer.enumerate.ParticipationType;
import com.mogakjak.mogakjak.domain.timer.enumerate.PomodoroPhaseType;
import com.mogakjak.mogakjak.domain.timer.repository.ActiveFocusSessionRepository;
import com.mogakjak.mogakjak.domain.timer.repository.FocusIntervalRepository;
import com.mogakjak.mogakjak.domain.timer.repository.FocusSessionRepository;
import com.mogakjak.mogakjak.domain.todo.entity.Todo;
import com.mogakjak.mogakjak.domain.todo.repository.TodoRepository;
import com.mogakjak.mogakjak.domain.user.entity.Category;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.repository.UserGroupRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserRepository;
import com.mogakjak.mogakjak.global.websocket.service.GroupMemberStatusService;
import com.mogakjak.mogakjak.global.websocket.service.TimerCompletionNotificationService;
import com.mogakjak.mogakjak.global.websocket.service.UserActiveStatusService;
import com.mogakjak.mogakjak.domain.lounge.service.OfficialLoungeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FocusSessionServiceImplTest {

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

    @InjectMocks
    private FocusSessionServiceImpl service;

    @Test
    void nextPomodoroPhase_doesNotAddPausedIntervalTwice() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        LocalDateTime startedAt = LocalDateTime.now().minusSeconds(20);
        LocalDateTime pausedAt = startedAt.plusSeconds(10);
        User user = User.builder().name("user").email("user@example.com").build();
        ReflectionTestUtils.setField(user, "id", userId);

        FocusSession session = createPomodoroSession(userId, startedAt);
        ReflectionTestUtils.setField(session, "id", sessionId);
        session.addDuration(10L); // pauseSession이 이미 반영한 구간
        session.pause(0);

        FocusInterval pausedFocusInterval = FocusInterval.create(
                sessionId, startedAt, PomodoroPhaseType.FOCUS, 1
        );
        pausedFocusInterval.end(pausedAt);

        ActiveFocusSession activeSession = mock(ActiveFocusSession.class);
        when(activeSession.getSessionId()).thenReturn(sessionId);
        when(activeFocusSessionRepository.findByUserId(userId))
                .thenReturn(Optional.of(activeSession));
        when(focusSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(focusIntervalRepository.findTopBySessionIdOrderByStartedAtDesc(sessionId))
                .thenReturn(Optional.of(pausedFocusInterval));
        when(focusIntervalRepository.findAllBySessionId(sessionId)).thenReturn(List.of(pausedFocusInterval));
        when(focusIntervalRepository.save(any(FocusInterval.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.nextPomodoroPhase(user, sessionId);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        assertEquals(10L, session.getTotalDuration());
    }

    private FocusSession createPomodoroSession(UUID userId, LocalDateTime startedAt) {
        Todo todo = mock(Todo.class);
        Category category = mock(Category.class);
        when(todo.getId()).thenReturn(UUID.randomUUID());
        when(todo.getCategory()).thenReturn(category);
        when(category.getId()).thenReturn(UUID.randomUUID());
        return FocusSession.createPomodoroSession(
                userId,
                todo,
                startedAt,
                10L,
                5L,
                2,
                ParticipationType.INDIVIDUAL,
                null,
                true,
                true
        );
    }
}
