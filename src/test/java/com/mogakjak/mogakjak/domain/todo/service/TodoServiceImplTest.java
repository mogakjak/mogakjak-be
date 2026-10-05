package com.mogakjak.mogakjak.domain.todo.service;

import com.mogakjak.mogakjak.domain.todo.controller.dto.CreateTodoRequest;
import com.mogakjak.mogakjak.domain.todo.controller.dto.TodoResponse;
import com.mogakjak.mogakjak.domain.todo.controller.dto.UpdateTodoRequest;
import com.mogakjak.mogakjak.domain.todo.entity.Todo;
import com.mogakjak.mogakjak.domain.todo.repository.TodoRepository;
import com.mogakjak.mogakjak.domain.user.entity.Category;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.repository.CategoryRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TodoServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private TodoRepository todoRepository;
    @InjectMocks private TodoServiceImpl service;

    private final UUID userId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();
    private final UUID todoId = UUID.randomUUID();
    private final LocalDate date = LocalDate.of(2026, 10, 5);
    private User user;
    private Category category;

    @BeforeEach
    void setUp() {
        user = User.builder().build();
        category = Category.builder().user(user).build();
        ReflectionTestUtils.setField(category, "id", categoryId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    }

    @Test
    void createTodoSavesUnsetTargetAndReturnsNullProgress() {
        when(categoryRepository.findByIdAndUserAndIsDeletedFalse(categoryId, user)).thenReturn(Optional.of(category));
        when(todoRepository.save(any(Todo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TodoResponse response = service.createTodo(userId, new CreateTodoRequest(categoryId, "독서", date, null));

        assertNull(response.getTargetTimeInSeconds());
        assertNull(response.getProgressRate());
        assertEquals(0, response.getActualTimeInSeconds());
    }

    @Test
    void updateTodoClearsExistingTargetWithoutLosingAccumulatedTime() {
        Todo todo = Todo.builder().user(user).category(category).task("독서").date(date)
                .targetTimeInSeconds(3600).actualTimeInSeconds(600).build();
        when(todoRepository.findByIdAndUserAndIsDeletedFalse(todoId, user)).thenReturn(Optional.of(todo));

        TodoResponse response = service.updateTodo(userId, todoId,
                new UpdateTodoRequest(categoryId, "독서", date, null));

        assertNull(todo.getTargetTimeInSeconds());
        assertNull(response.getProgressRate());
        assertEquals(600, response.getActualTimeInSeconds());
        assertEquals(date, response.getDate());
    }

    @Test
    void updateTodoSetsTargetOnPreviouslyUnsetTodo() {
        Todo todo = Todo.builder().user(user).category(category).task("독서").date(date)
                .actualTimeInSeconds(600).build();
        when(todoRepository.findByIdAndUserAndIsDeletedFalse(todoId, user)).thenReturn(Optional.of(todo));

        TodoResponse response = service.updateTodo(userId, todoId,
                new UpdateTodoRequest(categoryId, "독서", date, 1200));

        assertEquals(1200, response.getTargetTimeInSeconds());
        assertEquals(50, response.getProgressRate());
        assertEquals(600, response.getActualTimeInSeconds());
    }
}
