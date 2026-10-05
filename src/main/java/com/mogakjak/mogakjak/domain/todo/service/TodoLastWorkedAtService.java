package com.mogakjak.mogakjak.domain.todo.service;

import com.mogakjak.mogakjak.domain.timer.enumerate.PomodoroPhaseType;
import com.mogakjak.mogakjak.domain.timer.enumerate.TimerStatus;
import com.mogakjak.mogakjak.domain.timer.repository.FocusIntervalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoLastWorkedAtService {

    private static final List<PomodoroPhaseType> FOCUS_PHASES = List.of(PomodoroPhaseType.NORMAL, PomodoroPhaseType.FOCUS);
    private final FocusIntervalRepository focusIntervalRepository;

    public Map<UUID, LocalDateTime> getLastWorkedAt(UUID userId, Collection<UUID> todoIds) {
        return getLastWorkedAt(userId, todoIds, LocalDateTime.now());
    }

    public Map<UUID, LocalDateTime> getLastWorkedAt(UUID userId, Collection<UUID> todoIds, LocalDateTime now) {
        if (todoIds.isEmpty()) return Map.of();

        List<UUID> ids = todoIds.stream().distinct().toList();
        Map<UUID, LocalDateTime> result = new HashMap<>();
        focusIntervalRepository.findLastCompletedWorkByTodos(userId, ids, FOCUS_PHASES, now)
                .forEach(row -> result.put(row.getTodoId(), row.getLastWorkedAt()));
        focusIntervalRepository.findCurrentlyWorkingTodoIds(userId, ids, FOCUS_PHASES, TimerStatus.RUNNING, now)
                .forEach(todoId -> result.put(todoId, now));
        return result;
    }
}
