package com.mogakjak.mogakjak.domain.todo.controller;

import com.mogakjak.mogakjak.domain.todo.controller.dto.*;
import com.mogakjak.mogakjak.domain.todo.service.*;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.global.auth.security.CustomUserDetails;
import com.mogakjak.mogakjak.global.exception.*;
import com.mogakjak.mogakjak.global.exception.status.ErrorCode;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.OffsetDateTime;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TodoDetailControllerTest {
    private final TodoSidebarService sidebar = mock(TodoSidebarService.class);
    private final TodoService todos = mock(TodoService.class);
    private final UUID userId = UUID.randomUUID(), todoId = UUID.randomUUID();
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", userId);
        var details = CustomUserDetails.of(user);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(details, "", details.getAuthorities()));
        mvc = MockMvcBuilders.standaloneSetup(new TodoController(todos, sidebar))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void detailsUseAuthenticatedUserAndSerializeNullableDefaults() throws Exception {
        when(sidebar.getTodoDetail(userId, todoId)).thenReturn(new TodoDetailResponse(
                TodoResponse.builder().id(todoId).task("독서").actualTimeInSeconds(600).build(),
                CategoryResponse.builder().name("기본").build(), 600, null, true, true, null,
                OffsetDateTime.parse("2026-10-05T12:00:00+09:00")));
        mvc.perform(get("/api/todos/" + todoId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.todo.id").value(todoId.toString()))
                .andExpect(jsonPath("$.data.category.name").value("기본"))
                .andExpect(jsonPath("$.data.accumulatedTimeInSeconds").value(600))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"activeSession\":null")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"targetTimeInSeconds\":null")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"progressRate\":null")))
                .andExpect(jsonPath("$.data.isTaskPublic").value(true))
                .andExpect(jsonPath("$.data.serverTime").value("2026-10-05T12:00:00+09:00"));
        verify(sidebar).getTodoDetail(userId, todoId);
        verifyNoInteractions(todos);
    }

    @Test
    void foreignOrDeletedTodoReturnsExisting403Contract() throws Exception {
        when(sidebar.getTodoDetail(userId, todoId)).thenThrow(new CustomException(ErrorCode.FORBIDDEN_TODO_ACCESS));
        mvc.perform(get("/api/todos/" + todoId)).andExpect(status().isForbidden());
    }

    @Test
    void staticCategoryRouteStillUsesExistingControllerMethod() throws Exception {
        when(todos.getCategories(userId)).thenReturn(List.of());
        mvc.perform(get("/api/todos/categories")).andExpect(status().isOk());
        verifyNoInteractions(sidebar);
    }
}
