package com.mogakjak.mogakjak.domain.todo.controller;

import com.mogakjak.mogakjak.domain.todo.controller.dto.*;
import com.mogakjak.mogakjak.domain.todo.service.*;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.global.auth.security.CustomUserDetails;
import com.mogakjak.mogakjak.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TodoSearchControllerTest {
    private final TodoSearchService search = mock(TodoSearchService.class);
    private final TodoSidebarService sidebar = mock(TodoSidebarService.class);
    private final TodoService todos = mock(TodoService.class);
    private final UUID userId = UUID.randomUUID();
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", userId);
        var details = CustomUserDetails.of(user);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(details, "", details.getAuthorities()));
        mvc = MockMvcBuilders.standaloneSetup(new TodoController(todos, sidebar, search))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void searchStaticRouteUsesAuthenticatedUserAndResultContract() throws Exception {
        var todo = TodoResponse.builder().id(UUID.randomUUID()).task("가방")
                .actualTimeInSeconds(600).lastWorkedAt(OffsetDateTime.parse("2026-10-05T12:00:00+09:00")).build();
        when(search.search(eq(userId), any())).thenReturn(new TodoSearchResponse(
                List.of(new TodoSearchResponse.Item(todo, CategoryResponse.builder().name("기본").build())), false, null));
        mvc.perform(get("/api/todos/search").param("keyword", "ㄱㅂ").param("userId", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].todo.task").value("가방"))
                .andExpect(jsonPath("$.data.items[0].category.name").value("기본"))
                .andExpect(jsonPath("$.data.items[0].todo.lastWorkedAt").value("2026-10-05T12:00:00+09:00"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"targetTimeInSeconds\":null")))
                .andExpect(jsonPath("$.data.hasNext").value(false));
        var request = ArgumentCaptor.forClass(TodoSearchRequest.class);
        verify(search).search(eq(userId), request.capture());
        assertEquals("ㄱㅂ", request.getValue().getKeyword());
        assertEquals(20, request.getValue().getLimit());
        verifyNoInteractions(sidebar, todos);
    }

    @Test
    void omittedKeywordAndLimitUseBrowseDefaults() throws Exception {
        when(search.search(eq(userId), any())).thenReturn(new TodoSearchResponse(List.of(), false, null));
        mvc.perform(get("/api/todos/search")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"nextCursor\":null")));
        var request = ArgumentCaptor.forClass(TodoSearchRequest.class);
        verify(search).search(eq(userId), request.capture());
        assertEquals("", request.getValue().getKeyword());
        assertEquals(20, request.getValue().getLimit());
    }

    static Stream<Arguments> invalidParameters() {
        return Stream.of(Arguments.of("limit","0"), Arguments.of("limit","-1"),
                Arguments.of("limit","101"), Arguments.of("limit",""), Arguments.of("limit","abc"),
                Arguments.of("cursor","invalid-uuid"), Arguments.of("keyword","가".repeat(36)));
    }

    @ParameterizedTest
    @MethodSource("invalidParameters")
    void badParametersReturn400WithoutSearching(String name, String value) throws Exception {
        mvc.perform(get("/api/todos/search").param(name, value)).andExpect(status().isBadRequest());
        verifyNoInteractions(search, sidebar, todos);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 100})
    void limitBoundsAndUuidCursorAreAccepted(int limit) throws Exception {
        UUID cursor = UUID.randomUUID();
        when(search.search(eq(userId), any())).thenReturn(new TodoSearchResponse(List.of(), false, null));
        mvc.perform(get("/api/todos/search").param("limit", Integer.toString(limit))
                .param("cursor", cursor.toString())).andExpect(status().isOk());
        var request = ArgumentCaptor.forClass(TodoSearchRequest.class);
        verify(search).search(eq(userId), request.capture());
        assertEquals(limit, request.getValue().getLimit());
        assertEquals(cursor, request.getValue().getCursor());
    }
}
