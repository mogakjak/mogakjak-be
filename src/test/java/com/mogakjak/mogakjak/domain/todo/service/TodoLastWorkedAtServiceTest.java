package com.mogakjak.mogakjak.domain.todo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mogakjak.mogakjak.domain.timer.enumerate.PomodoroPhaseType;
import com.mogakjak.mogakjak.domain.timer.enumerate.TimerStatus;
import com.mogakjak.mogakjak.domain.timer.repository.FocusIntervalRepository;
import com.mogakjak.mogakjak.domain.timer.repository.TodoLastWorkedAtProjection;
import com.mogakjak.mogakjak.domain.todo.controller.dto.TodoResponse;
import com.mogakjak.mogakjak.domain.todo.entity.Todo;
import com.mogakjak.mogakjak.domain.user.entity.Category;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TodoLastWorkedAtServiceTest {
    @Mock private FocusIntervalRepository repository;
    @InjectMocks private TodoLastWorkedAtService service;
    private final UUID userId = UUID.randomUUID();
    private final List<PomodoroPhaseType> phases = List.of(PomodoroPhaseType.NORMAL, PomodoroPhaseType.FOCUS);

    @Test
    void emptyListDoesNotQuery() {
        assertTrue(service.getLastWorkedAt(userId, List.of()).isEmpty());
        verifyNoInteractions(repository);
    }

    @Test
    void batchQueriesOnceWithDistinctIdsAndOneSharedNow() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        LocalDateTime old = LocalDateTime.of(2026, 10, 1, 12, 0);
        TodoLastWorkedAtProjection row = mock(TodoLastWorkedAtProjection.class);
        when(row.getTodoId()).thenReturn(first);
        when(row.getLastWorkedAt()).thenReturn(old);
        when(repository.findLastCompletedWorkByTodos(eq(userId), eq(List.of(first, second)), eq(phases), any()))
                .thenReturn(List.of(row));
        when(repository.findCurrentlyWorkingTodoIds(eq(userId), eq(List.of(first, second)), eq(phases),
                eq(TimerStatus.RUNNING), any())).thenReturn(List.of(first));
        LocalDateTime before = LocalDateTime.now();

        var result = service.getLastWorkedAt(userId, List.of(first, second, first));

        ArgumentCaptor<LocalDateTime> completedNow = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> runningNow = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repository).findLastCompletedWorkByTodos(eq(userId), eq(List.of(first, second)), eq(phases), completedNow.capture());
        verify(repository).findCurrentlyWorkingTodoIds(eq(userId), eq(List.of(first, second)), eq(phases),
                eq(TimerStatus.RUNNING), runningNow.capture());
        assertEquals(completedNow.getValue(), runningNow.getValue());
        assertEquals(runningNow.getValue(), result.get(first));
        assertFalse(result.get(first).isBefore(before));
        assertFalse(result.containsKey(second));
        verifyNoMoreInteractions(repository);
    }

    @Test
    void completedTimestampIsUnchangedAndMissingHistoryIsAbsent() {
        UUID todoId = UUID.randomUUID();
        TodoLastWorkedAtProjection row = mock(TodoLastWorkedAtProjection.class);
        LocalDateTime end = LocalDateTime.of(2026, 10, 1, 23, 59);
        when(row.getTodoId()).thenReturn(todoId);
        when(row.getLastWorkedAt()).thenReturn(end);
        when(repository.findLastCompletedWorkByTodos(eq(userId), any(), eq(phases), any())).thenReturn(List.of(row));
        assertEquals(end, service.getLastWorkedAt(userId, List.of(todoId, UUID.randomUUID())).get(todoId));
    }

    @ParameterizedTest
    @ValueSource(strings = {"UTC", "Asia/Seoul"})
    void responseConvertsServerLocalTimeToKoreanOffsetWithoutChangingInstant(String zone) throws Exception {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(zone));
            // This instant is after midnight in Seoul, even when the server runs in UTC.
            var instant = java.time.Instant.parse("2026-10-04T15:05:00Z");
            LocalDateTime serverLocal = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            Todo todo = Todo.builder().category(Category.builder().build()).task("독서").build();
            TodoResponse response = TodoResponse.from(todo, serverLocal);
            assertEquals(instant, response.getLastWorkedAt().toInstant());
            assertEquals(ZoneOffset.ofHours(9), response.getLastWorkedAt().getOffset());
            assertEquals(5, response.getLastWorkedAt().getDayOfMonth());
            ObjectMapper mapper = new ObjectMapper().findAndRegisterModules()
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            assertEquals("2026-10-05T00:05:00+09:00",
                    mapper.readTree(mapper.writeValueAsString(response)).get("lastWorkedAt").asText());
            assertTrue(mapper.readTree(mapper.writeValueAsString(TodoResponse.from(todo))).get("lastWorkedAt").isNull());
        } finally {
            TimeZone.setDefault(original);
        }
    }
}
