package com.mogakjak.mogakjak.domain.todo.service;

import com.mogakjak.mogakjak.domain.timer.entity.FocusSession;
import com.mogakjak.mogakjak.domain.timer.enumerate.TimerStatus;
import com.mogakjak.mogakjak.domain.timer.repository.ActiveFocusSessionRepository;
import com.mogakjak.mogakjak.domain.timer.repository.FocusIntervalRepository;
import com.mogakjak.mogakjak.domain.timer.repository.FocusSessionRepository;
import com.mogakjak.mogakjak.domain.timer.service.FocusSessionSnapshotCalculator;
import com.mogakjak.mogakjak.domain.timer.service.TodoAccumulatedTimeCalculator;
import com.mogakjak.mogakjak.domain.todo.controller.dto.ActiveTodoSessionResponse;
import com.mogakjak.mogakjak.domain.todo.controller.dto.CategoryResponse;
import com.mogakjak.mogakjak.domain.todo.controller.dto.TodoDetailResponse;
import com.mogakjak.mogakjak.domain.todo.controller.dto.TodoResponse;
import com.mogakjak.mogakjak.domain.todo.repository.TodoRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserRepository;
import com.mogakjak.mogakjak.global.exception.CustomException;
import com.mogakjak.mogakjak.global.exception.status.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoSidebarService {
    private final UserRepository userRepository;
    private final TodoRepository todoRepository;
    private final ActiveFocusSessionRepository activeFocusSessionRepository;
    private final FocusSessionRepository focusSessionRepository;
    private final FocusIntervalRepository focusIntervalRepository;
    private final TodoLastWorkedAtService todoLastWorkedAtService;

    public TodoDetailResponse getTodoDetail(UUID userId, UUID todoId) {
        var user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        var todo = todoRepository.findByIdAndUserAndIsDeletedFalse(todoId, user)
                .orElseThrow(() -> new CustomException(ErrorCode.FORBIDDEN_TODO_ACCESS));
        var category = todo.getCategory();
        if (Boolean.TRUE.equals(category.getIsDeleted()) || !userId.equals(category.getUser().getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN_TODO_ACCESS);
        }
        LocalDateTime now = LocalDateTime.now();
        FocusSession session = activeFocusSessionRepository.findByUserId(userId)
                .filter(active -> userId.equals(active.getUserId()))
                .flatMap(active -> focusSessionRepository.findById(active.getSessionId()))
                .filter(active -> userId.equals(active.getUserId()) && todoId.equals(active.getTodoId()))
                .filter(active -> active.getStatus() == TimerStatus.RUNNING || active.getStatus() == TimerStatus.PAUSED)
                .orElse(null);
        long accumulated = todo.getActualTimeInSeconds() == null ? 0 : todo.getActualTimeInSeconds();
        ActiveTodoSessionResponse activeResponse = null;
        if (session != null) {
            var intervals = focusIntervalRepository.findAllBySessionId(session.getId());
            var snapshot = FocusSessionSnapshotCalculator.calculate(session, intervals, now);
            accumulated = TodoAccumulatedTimeCalculator.calculate(todo, session, snapshot.currentInterval(), now);
            activeResponse = ActiveTodoSessionResponse.from(session, snapshot);
        }
        var lastWorkedAt = todoLastWorkedAtService.getLastWorkedAt(userId, List.of(todoId), now).get(todoId);
        return new TodoDetailResponse(TodoResponse.from(todo, lastWorkedAt), CategoryResponse.from(category),
                accumulated, todo.calculateProgressRate(accumulated),
                session == null || !Boolean.FALSE.equals(session.getIsTaskPublic()),
                session == null || !Boolean.FALSE.equals(session.getIsTimerPublic()), activeResponse,
                now.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("Asia/Seoul")).toOffsetDateTime());
    }
}
