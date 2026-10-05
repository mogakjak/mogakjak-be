package com.mogakjak.mogakjak.domain.todo.service;

import com.mogakjak.mogakjak.domain.todo.controller.dto.CategoryResponse;
import com.mogakjak.mogakjak.domain.todo.controller.dto.TodoResponse;
import com.mogakjak.mogakjak.domain.todo.controller.dto.TodoSearchRequest;
import com.mogakjak.mogakjak.domain.todo.controller.dto.TodoSearchResponse;
import com.mogakjak.mogakjak.domain.todo.entity.Todo;
import com.mogakjak.mogakjak.domain.todo.repository.TodoRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserRepository;
import com.mogakjak.mogakjak.global.exception.CustomException;
import com.mogakjak.mogakjak.global.exception.status.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoSearchService {
    private static final int CANDIDATE_BATCH_SIZE = 256;
    private final UserRepository userRepository;
    private final TodoRepository todoRepository;
    private final TodoLastWorkedAtService todoLastWorkedAtService;

    public TodoSearchResponse search(UUID userId, TodoSearchRequest request) {
        var user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        LocalDateTime afterCreatedAt = null;
        UUID afterId = null;
        if (request.getCursor() != null) {
            Todo cursor = todoRepository.findByIdAndUserAndIsDeletedFalse(request.getCursor(), user)
                    .filter(todo -> !Boolean.TRUE.equals(todo.getCategory().getIsDeleted()))
                    .filter(todo -> userId.equals(todo.getCategory().getUser().getId()))
                    .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT_VALUE));
            afterCreatedAt = cursor.getCreatedAt();
            afterId = cursor.getId();
        }
        var matcher = new HangulTodoMatcher(request.getKeyword());
        var matches = new ArrayList<UUID>();
        // A single look-ahead match determines hasNext without a COUNT query.
        while (matches.size() <= request.getLimit()) {
            var candidates = todoRepository.findSearchCandidates(userId, afterCreatedAt, afterId,
                    PageRequest.of(0, CANDIDATE_BATCH_SIZE));
            for (var candidate : candidates) {
                if (matcher.matches(candidate.getTask())) matches.add(candidate.getId());
                if (matches.size() > request.getLimit()) break;
            }
            if (matches.size() > request.getLimit() || candidates.size() < CANDIDATE_BATCH_SIZE) break;
            // Advance over non-matching titles too; never rescan a candidate batch.
            var last = candidates.getLast();
            afterCreatedAt = last.getCreatedAt();
            afterId = last.getId();
        }
        boolean hasNext = matches.size() > request.getLimit();
        List<UUID> ids = List.copyOf(matches.subList(0, Math.min(matches.size(), request.getLimit())));
        if (ids.isEmpty()) return new TodoSearchResponse(List.of(), false, null);

        var byId = todoRepository.findSearchResults(userId, ids).stream()
                .collect(Collectors.toMap(Todo::getId, Function.identity()));
        var lastWorked = todoLastWorkedAtService.getLastWorkedAt(userId, ids);
        var items = ids.stream().map(byId::get).filter(Objects::nonNull)
                .map(todo -> new TodoSearchResponse.Item(TodoResponse.from(todo, lastWorked.get(todo.getId())),
                        CategoryResponse.from(todo.getCategory()))).toList();
        return new TodoSearchResponse(items, hasNext, hasNext ? ids.getLast() : null);
    }
}
