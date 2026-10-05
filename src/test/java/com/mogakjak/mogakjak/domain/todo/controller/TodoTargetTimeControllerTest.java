package com.mogakjak.mogakjak.domain.todo.controller;

import com.mogakjak.mogakjak.domain.todo.controller.dto.TodoResponse;
import com.mogakjak.mogakjak.domain.todo.controller.dto.UpdateTodoTargetTimeRequest;
import com.mogakjak.mogakjak.domain.todo.service.TodoService;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.global.auth.security.CustomUserDetails;
import com.mogakjak.mogakjak.global.exception.CustomException;
import com.mogakjak.mogakjak.global.exception.GlobalExceptionHandler;
import com.mogakjak.mogakjak.global.exception.status.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TodoTargetTimeControllerTest {

    private final TodoService service = mock(TodoService.class);
    private final UUID userId = UUID.randomUUID();
    private final UUID todoId = UUID.randomUUID();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", userId);
        CustomUserDetails details = CustomUserDetails.of(user);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(details, "", details.getAuthorities()));
        mvc = MockMvcBuilders.standaloneSetup(new TodoController(service,
                        mock(com.mogakjak.mogakjak.domain.todo.service.TodoSidebarService.class),
                        mock(com.mogakjak.mogakjak.domain.todo.service.TodoSearchService.class)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void setTargetReturnsUpdatedTodoAndUsesAuthenticatedUser() throws Exception {
        when(service.updateTodoTargetTime(eq(userId), eq(todoId), any()))
                .thenReturn(TodoResponse.builder().id(todoId).targetTimeInSeconds(3600).progressRate(25).build());

        mvc.perform(patch(path()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetTimeInSeconds\":3600}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.targetTimeInSeconds").value(3600))
                .andExpect(jsonPath("$.data.progressRate").value(25));

        ArgumentCaptor<UpdateTodoTargetTimeRequest> request = ArgumentCaptor.forClass(UpdateTodoTargetTimeRequest.class);
        verify(service).updateTodoTargetTime(eq(userId), eq(todoId), request.capture());
        assertTrue(request.getValue().isTargetTimeProvided());
        assertEquals(3600, request.getValue().getTargetTimeInSeconds());
    }

    @Test
    void explicitNullClearsTarget() throws Exception {
        when(service.updateTodoTargetTime(eq(userId), eq(todoId), any())).thenReturn(TodoResponse.builder().id(todoId).build());

        mvc.perform(patch(path()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetTimeInSeconds\":null}"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"targetTimeInSeconds\":null")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"progressRate\":null")));

        ArgumentCaptor<UpdateTodoTargetTimeRequest> request = ArgumentCaptor.forClass(UpdateTodoTargetTimeRequest.class);
        verify(service).updateTodoTargetTime(eq(userId), eq(todoId), request.capture());
        assertTrue(request.getValue().isTargetTimeProvided());
        assertNull(request.getValue().getTargetTimeInSeconds());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"targetTimeProvided\":true}", "{\"targetTimeInSeconds\":0}",
            "{\"targetTimeInSeconds\":59}", "{\"targetTimeInSeconds\":86401}",
            "{\"targetTimeInSeconds\":\"invalid\"}", "{", "[]", "null"})
    void invalidRequestReturns400AndDoesNotMutateTodo(String json) throws Exception {
        mvc.perform(patch(path()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));
        verifyNoInteractions(service);
    }

    @Test
    void ownershipErrorKeepsExisting403Contract() throws Exception {
        when(service.updateTodoTargetTime(eq(userId), eq(todoId), any()))
                .thenThrow(new CustomException(ErrorCode.FORBIDDEN_TODO_ACCESS));
        mvc.perform(patch(path()).contentType(MediaType.APPLICATION_JSON).content("{\"targetTimeInSeconds\":3600}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unexpectedServerErrorStillReturns500() throws Exception {
        when(service.updateTodoTargetTime(eq(userId), eq(todoId), any())).thenThrow(new IllegalStateException("test error"));
        mvc.perform(patch(path()).contentType(MediaType.APPLICATION_JSON).content("{\"targetTimeInSeconds\":3600}"))
                .andExpect(status().isInternalServerError());
    }

    @ParameterizedTest
    @ValueSource(ints = {60, 86400})
    void inclusiveRangeBoundariesAreAccepted(int seconds) throws Exception {
        when(service.updateTodoTargetTime(eq(userId), eq(todoId), any()))
                .thenReturn(TodoResponse.builder().targetTimeInSeconds(seconds).build());
        mvc.perform(patch(path()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetTimeInSeconds\":" + seconds + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.targetTimeInSeconds").value(seconds));
    }

    private String path() {
        return "/api/todos/" + todoId + "/target-time";
    }
}
