package com.mogakjak.mogakjak.domain.todo.controller;

import com.mogakjak.mogakjak.domain.todo.controller.dto.*;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.global.auth.security.CustomUserDetails;
import com.mogakjak.mogakjak.global.auth.security.resolver.CurrentUser;
import com.mogakjak.mogakjak.global.common.ApiResponse;
import com.mogakjak.mogakjak.global.exception.status.SuccessCode;
import com.mogakjak.mogakjak.domain.todo.service.TodoService;
import com.mogakjak.mogakjak.domain.todo.service.TodoSidebarService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Tag(name = "To-do", description = "To-do 관련 API")
@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;
    private final TodoSidebarService todoSidebarService;

    @Operation(summary = "선택한 할 일의 사이드바 상세 조회",
            description = "본인 소유·미삭제 할 일과 동일 할 일의 활성 RUNNING/PAUSED 세션을 조회합니다. 저장 누적시간과 진행 중 집중시간을 구분하며 휴식은 집중시간에서 제외합니다. 세션 없으면 activeSession은 null, 공개 기본값은 true입니다. 조회는 저장하지 않습니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "선택한 할 일 상세",
            content = @Content(examples = @ExampleObject(name = "목표시간·활성 세션 없음", value = """
                    {"statusCode":200,"data":{"todo":{"id":"7f000001-9a3d-1f34-819a-3d92e3800001","task":"독서","targetTimeInSeconds":null,"actualTimeInSeconds":600,"progressRate":null,"lastWorkedAt":null},"category":{"name":"기본"},"accumulatedTimeInSeconds":600,"progressRate":null,"isTaskPublic":true,"isTimerPublic":true,"activeSession":null,"serverTime":"2026-10-05T12:34:56+09:00"}}
                    """)))
    @GetMapping("/{todoId}")
    public ApiResponse<TodoDetailResponse> getTodoDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable UUID todoId) {
        return ApiResponse.success(SuccessCode.OK,
                todoSidebarService.getTodoDetail(getUserId(userDetails), todoId));
    }

    @Operation(summary = "카테고리 생성", description = "새로운 카테고리를 생성합니다.")
    @PostMapping("/categories")
    public ApiResponse<CategoryResponse> createCategory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateCategoryRequest createCategoryRequest) {

        UUID userId = getUserId(userDetails);
        CategoryResponse categoryResponse = todoService.createCategory(userId, createCategoryRequest);
        return ApiResponse.success(SuccessCode.CREATED, categoryResponse);
    }

    @Operation(summary = "카테고리 수정", description = "카테고리를 수정합니다.")
    @PutMapping("/categories")
    public ApiResponse<CategoryResponse> modifyCategory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateCategoryRequest updateCategoryRequest) {

        UUID userId = getUserId(userDetails);
        CategoryResponse categoryResponse = todoService.modifyCategory(userId, updateCategoryRequest);
        return ApiResponse.success(SuccessCode.CREATED, categoryResponse);
    }

    @Operation(summary = "카테고리 순서 변경", description = "카테고리 목록의 순서를 일괄 변경합니다.")
    @PatchMapping("/categories/order")
    public ApiResponse<Void> updateCategoryOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateCategoryOrderRequest updateCategoryOrderRequest) {

        UUID userId = getUserId(userDetails);
        todoService.updateCategoryOrder(userId, updateCategoryOrderRequest);
        return ApiResponse.success(SuccessCode.OK);
    }

    @Operation(summary = "카테고리 목록 조회", description = "사용자의 모든 카테고리 목록을 순서대로 조회합니다.")
    @GetMapping("/categories")
    public ApiResponse<List<CategoryResponse>> getCategories(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UUID userId = getUserId(userDetails);
        List<CategoryResponse> categories = todoService.getCategories(userId);
        return ApiResponse.success(SuccessCode.OK, categories);
    }

    @Operation(summary = "카테고리 삭제", description = "카테고리를 삭제합니다. 해당 카테고리의 모든 할 일(Todo)도 함께 삭제됩니다.")
    @DeleteMapping("/categories/{categoryId}")
    public ApiResponse<Void> deleteCategory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID categoryId) {

        UUID userId = getUserId(userDetails);
        todoService.deleteCategory(userId, categoryId);
        return ApiResponse.success(SuccessCode.OK);
    }

    /*
     * == Todo (할 일) API ==
     */

    @Operation(summary = "유저의 To-do 전체 조회", description = "로그인한 유저의 할 일 목록을 조회합니다. 최근 만들어진 순으로 정렬됩니다.")
    @GetMapping("/my")
    public ApiResponse<List<TodoResponse>> getUserTodos(
            @CurrentUser User user
    ) {
        List<TodoResponse> todos = todoService.getUserTodos(user);
        return ApiResponse.success(SuccessCode.OK, todos);
    }

    @Operation(summary = "'오늘'의 To-do 목록 조회", description = "오늘 날짜의 모든 카테고리와 할 일 목록을 조회합니다.")
    @GetMapping("/today")
    public ApiResponse<List<CategoryWithTodosResponse>> getTodayTodos(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UUID userId = getUserId(userDetails);
        List<CategoryWithTodosResponse> todayTodos = todoService.getTodosGroupedByCategory(userId, LocalDate.now());
        return ApiResponse.success(SuccessCode.OK, todayTodos);
    }

    @Operation(summary = "특정 날짜의 To-do 목록 조회", description = "특정 날짜의 모든 카테고리와 할 일 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<List<CategoryWithTodosResponse>> getTodosByDate(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("date") LocalDate date) {

        UUID userId = getUserId(userDetails);
        List<CategoryWithTodosResponse> todos = todoService.getTodosGroupedByCategory(userId, date);
        return ApiResponse.success(SuccessCode.OK, todos);
    }

    @Operation(summary = "할 일(Todo) 생성", description = "특정 카테고리에 새로운 할 일을 추가합니다.")
    @PostMapping
    public ApiResponse<TodoResponse> createTodo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateTodoRequest createTodoRequest) {

        UUID userId = getUserId(userDetails);
        TodoResponse newTodo = todoService.createTodo(userId, createTodoRequest);
        return ApiResponse.success(SuccessCode.CREATED, newTodo);
    }

    @Operation(summary = "할 일(Todo) 수정", description = "기존 할 일의 내용을 수정합니다.")
    @PutMapping("/{todoId}")
    public ApiResponse<TodoResponse> updateTodo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID todoId,
            @Valid @RequestBody UpdateTodoRequest updateTodoRequest) {

        UUID userId = getUserId(userDetails);
        TodoResponse updatedTodo = todoService.updateTodo(userId, todoId, updateTodoRequest);
        return ApiResponse.success(SuccessCode.OK, updatedTodo);
    }

    @Operation(summary = "할 일 목표시간 단독 변경", description = "목표시간만 설정·변경합니다. targetTimeInSeconds 필드는 필수이고, null은 목표시간을 해제합니다. 변경한 값은 기존 할 일 목록 API로 다시 조회할 수 있습니다.")
    @PatchMapping("/{todoId}/target-time")
    public ApiResponse<TodoResponse> updateTodoTargetTime(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID todoId,
            @Valid @RequestBody UpdateTodoTargetTimeRequest request) {
        TodoResponse response = todoService.updateTodoTargetTime(getUserId(userDetails), todoId, request);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @Operation(summary = "할 일(Todo) 완료/미완료 토글", description = "할 일의 완료 상태를 토글합니다.")
    @PatchMapping("/{todoId}/complete")
    public ApiResponse<TodoResponse> toggleTodoComplete(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID todoId) {

        UUID userId = getUserId(userDetails);
        TodoResponse toggledTodo = todoService.toggleTodoComplete(userId, todoId);
        return ApiResponse.success(SuccessCode.OK, toggledTodo);
    }

    @Operation(summary = "할 일(Todo) 삭제", description = "할 일을 삭제합니다.")
    @DeleteMapping("/{todoId}")
    public ApiResponse<Void> deleteTodo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID todoId) {

        UUID userId = getUserId(userDetails);
        todoService.deleteTodo(userId, todoId);
        return ApiResponse.success(SuccessCode.OK);
    }

    private UUID getUserId(CustomUserDetails userDetails) {
        return UUID.fromString(userDetails.getUsername());
    }
}
