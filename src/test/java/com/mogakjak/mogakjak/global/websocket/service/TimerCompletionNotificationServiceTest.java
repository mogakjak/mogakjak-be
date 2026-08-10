package com.mogakjak.mogakjak.global.websocket.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mogakjak.mogakjak.domain.timer.entity.FocusInterval;
import com.mogakjak.mogakjak.domain.timer.entity.FocusSession;
import com.mogakjak.mogakjak.domain.timer.enumerate.PomodoroPhaseType;
import com.mogakjak.mogakjak.domain.timer.enumerate.TimerMode;
import com.mogakjak.mogakjak.domain.timer.repository.FocusIntervalRepository;
import com.mogakjak.mogakjak.domain.timer.repository.FocusSessionRepository;
import com.mogakjak.mogakjak.domain.todo.repository.TodoRepository;
import com.mogakjak.mogakjak.global.websocket.dto.TimerCompletionNotificationDto;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimerCompletionNotificationServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private FocusSessionRepository focusSessionRepository;

    @Mock
    private FocusIntervalRepository focusIntervalRepository;

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private RealtimeEventPublisher realtimeEventPublisher;

    @InjectMocks
    private TimerCompletionNotificationService service;

    @BeforeEach
    void setUp() {
        service.init();
    }

    @AfterEach
    void tearDown() {
        service.destroy();
    }

    @Test
    void sendCompletionNotification_focusPhaseBeforeLastRoundPublishesBreakStartMessage() throws Exception {
        UUID sessionId = UUID.randomUUID();
        FocusSession focusSession = pomodoroSession(sessionId, 3);
        FocusInterval interval = interval(PomodoroPhaseType.FOCUS, 1);

        when(focusIntervalRepository.findTopBySessionIdOrderByStartedAtDesc(sessionId))
                .thenReturn(Optional.of(interval));

        service.sendCompletionNotification(focusSession);

        TimerCompletionNotificationDto notification = captureNotification();
        assertEquals("이제 휴식시간입니다!", notification.getMessage());
    }

    @Test
    void sendCompletionNotification_breakPhasePublishesFocusStartMessage() throws Exception {
        UUID sessionId = UUID.randomUUID();
        FocusSession focusSession = pomodoroSession(sessionId, 3);
        FocusInterval interval = interval(PomodoroPhaseType.BREAK, 1);

        when(focusIntervalRepository.findTopBySessionIdOrderByStartedAtDesc(sessionId))
                .thenReturn(Optional.of(interval));

        service.sendCompletionNotification(focusSession);

        TimerCompletionNotificationDto notification = captureNotification();
        assertEquals("휴식이 끝났어요. 다시 집중할 시간입니다!", notification.getMessage());
    }

    @Test
    void sendCompletionNotification_lastFocusPhasePublishesPomodoroCompletionMessage() throws Exception {
        UUID sessionId = UUID.randomUUID();
        FocusSession focusSession = pomodoroSession(sessionId, 3);
        FocusInterval interval = interval(PomodoroPhaseType.FOCUS, 3);

        when(focusIntervalRepository.findTopBySessionIdOrderByStartedAtDesc(sessionId))
                .thenReturn(Optional.of(interval));

        service.sendCompletionNotification(focusSession);

        TimerCompletionNotificationDto notification = captureNotification();
        assertEquals("뽀모도로가 완료되었습니다!", notification.getMessage());
    }

    private TimerCompletionNotificationDto captureNotification() throws Exception {
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(realtimeEventPublisher).publish(eq("timer-completion"), payloadCaptor.capture());
        return objectMapper.readValue(payloadCaptor.getValue(), TimerCompletionNotificationDto.class);
    }

    private FocusSession pomodoroSession(UUID sessionId, int repeatCount) {
        FocusSession focusSession = org.mockito.Mockito.mock(FocusSession.class);
        when(focusSession.getId()).thenReturn(sessionId);
        when(focusSession.getUserId()).thenReturn(UUID.randomUUID());
        when(focusSession.getMode()).thenReturn(TimerMode.POMODORO);
        org.mockito.Mockito.lenient().when(focusSession.getRepeatCount()).thenReturn(repeatCount);
        return focusSession;
    }

    private FocusInterval interval(PomodoroPhaseType phaseType, int round) {
        FocusInterval interval = org.mockito.Mockito.mock(FocusInterval.class);
        when(interval.getPhaseType()).thenReturn(phaseType);
        if (phaseType == PomodoroPhaseType.FOCUS) {
            when(interval.getRound()).thenReturn(round);
        }
        return interval;
    }
}
