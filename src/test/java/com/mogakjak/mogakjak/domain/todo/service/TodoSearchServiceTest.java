package com.mogakjak.mogakjak.domain.todo.service;

import com.mogakjak.mogakjak.domain.todo.controller.dto.TodoSearchRequest;
import com.mogakjak.mogakjak.domain.todo.entity.Todo;
import com.mogakjak.mogakjak.domain.todo.repository.*;
import com.mogakjak.mogakjak.domain.user.entity.*;
import com.mogakjak.mogakjak.domain.user.repository.UserRepository;
import com.mogakjak.mogakjak.global.exception.CustomException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TodoSearchServiceTest {
    @Mock private UserRepository users;
    @Mock private TodoRepository todos;
    @Mock private TodoLastWorkedAtService lastWorked;
    @InjectMocks private TodoSearchService service;
    private final UUID userId = UUID.randomUUID();
    private User user;

    @BeforeEach
    void setup() {
        user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", userId);
        when(users.findById(userId)).thenReturn(Optional.of(user));
    }

    @Test
    void pageHydratesOnceKeepsOrderAndFetchesHistoryInOneBatch() {
        Todo first = todo("가방"), second = todo("공부"), lookahead = todo("기록");
        second.updateTargetTime(3600);
        when(todos.findSearchCandidates(eq(userId), isNull(), isNull(), any()))
                .thenReturn(List.of(candidate(first), candidate(todo("나무")), candidate(second), candidate(lookahead)));
        List<UUID> ids = List.of(first.getId(), second.getId());
        when(todos.findSearchResults(userId, ids)).thenReturn(List.of(second, first));
        LocalDateTime end = LocalDateTime.now().minusDays(1);
        when(lastWorked.getLastWorkedAt(userId, ids)).thenReturn(Map.of(first.getId(), end));
        var result = service.search(userId, request("ㄱ", 2));
        assertEquals(ids, result.items().stream().map(item -> item.todo().getId()).toList());
        assertTrue(result.hasNext());
        assertEquals(second.getId(), result.nextCursor());
        assertNull(result.items().getFirst().todo().getTargetTimeInSeconds());
        assertNotNull(result.items().getFirst().todo().getLastWorkedAt());
        assertEquals("기본", result.items().getFirst().category().getName());
        verify(todos).findSearchCandidates(eq(userId), isNull(), isNull(), any());
        verify(todos).findSearchResults(userId, ids);
        verify(lastWorked).getLastWorkedAt(userId, ids);
        verifyNoMoreInteractions(todos, lastWorked);
    }

    @Test
    void noMatchesSkipsHydrationAndRecentWorkQueries() {
        when(todos.findSearchCandidates(eq(userId), isNull(), isNull(), any()))
                .thenReturn(List.of(candidate(todo("나무"))));
        var result = service.search(userId, request("ㄱ", 20));
        assertTrue(result.items().isEmpty());
        assertFalse(result.hasNext());
        assertNull(result.nextCursor());
        verify(todos, never()).findSearchResults(any(), any());
        verifyNoInteractions(lastWorked);
    }

    @Test
    void matchingAcrossCandidateBatchesUsesSeekNotOffsets() {
        var batch = IntStream.range(0, 256).mapToObj(i -> candidate(todo("나무"))).toList();
        var last = batch.getLast();
        Todo match = todo("가방");
        when(todos.findSearchCandidates(eq(userId), isNull(), isNull(), any())).thenReturn(batch);
        when(todos.findSearchCandidates(eq(userId), eq(last.getCreatedAt()), eq(last.getId()), any()))
                .thenReturn(List.of(candidate(match)));
        when(todos.findSearchResults(userId, List.of(match.getId()))).thenReturn(List.of(match));
        var result = service.search(userId, request("가", 20));
        assertEquals(match.getId(), result.items().getFirst().todo().getId());
        assertFalse(result.hasNext());
        var pages = ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(todos, times(2)).findSearchCandidates(eq(userId), any(), any(), pages.capture());
        assertTrue(pages.getAllValues().stream().allMatch(page -> page.getOffset() == 0 && page.getPageSize() == 256));
    }

    @Test
    void foreignMissingOrDeletedCursorIsRejectedBeforeScanning() {
        var request = request("", 20);
        request.setCursor(UUID.randomUUID());
        assertThrows(CustomException.class, () -> service.search(userId, request));
        verify(todos).findByIdAndUserAndIsDeletedFalse(request.getCursor(), user);
        verify(todos, never()).findSearchCandidates(any(), any(), any(), any());
        verifyNoInteractions(lastWorked);
    }

    @Test
    void validCursorUsesStoredCreatedTimeAndId() {
        Todo anchor = todo("가방");
        var request = request("ㄱ", 20);
        request.setCursor(anchor.getId());
        when(todos.findByIdAndUserAndIsDeletedFalse(anchor.getId(), user)).thenReturn(Optional.of(anchor));
        service.search(userId, request);
        verify(todos).findSearchCandidates(eq(userId), eq(anchor.getCreatedAt()), eq(anchor.getId()), any());
    }

    private TodoSearchRequest request(String keyword, int limit) {
        var request = new TodoSearchRequest();
        request.setKeyword(keyword);
        request.setLimit(limit);
        return request;
    }

    private Todo todo(String title) {
        var category = Category.builder().user(user).name("기본").build();
        ReflectionTestUtils.setField(category, "id", UUID.randomUUID());
        var todo = Todo.builder().user(user).category(category).task(title).actualTimeInSeconds(600).build();
        ReflectionTestUtils.setField(todo, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(todo, "createdAt", LocalDateTime.now());
        return todo;
    }

    private TodoSearchCandidate candidate(Todo todo) {
        return new TodoSearchCandidate() {
            public UUID getId() { return todo.getId(); }
            public String getTask() { return todo.getTask(); }
            public LocalDateTime getCreatedAt() { return todo.getCreatedAt(); }
        };
    }
}
