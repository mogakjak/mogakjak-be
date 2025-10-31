package com.mogakjak.mogakjak.controller;

import com.mogakjak.mogakjak.controller.dto.CategoryResponse;
import com.mogakjak.mogakjak.controller.dto.CategoryWithTodosResponse;
import com.mogakjak.mogakjak.controller.dto.CreateCategoryRequest;
import com.mogakjak.mogakjak.controller.dto.CreateTodoRequest;
import com.mogakjak.mogakjak.controller.dto.TodoResponse;
import com.mogakjak.mogakjak.controller.dto.UpdateCategoryOrderRequest;
import com.mogakjak.mogakjak.controller.dto.UpdateTodoRequest;
import com.mogakjak.mogakjak.domain.user.service.TodoService;
import com.mogakjak.mogakjak.global.auth.security.CustomUserDetails;
import com.mogakjak.mogakjak.global.common.ApiResponse;
import com.mogakjak.mogakjak.global.exception.status.SuccessCode;
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

    @Operation(summary = "카테고리 생성", description = "새로운 카테고리를 생성합니다.")
    @PostMapping("/categories")
    public ApiResponse<CategoryResponse> createCategory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateCategoryRequest createCategoryRequest) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        CategoryResponse categoryResponse = todoService.createCategory(userId, createCategoryRequest);
        return ApiResponse.success(SuccessCode.CREATED, categoryResponse);
    }

    @Operation(summary = "카테고리 순서 변경", description = "카테고리 목록의 순서를 일괄 변경합니다.")
    @PatchMapping("/categories/order")
    public ApiResponse<Void> updateCategoryOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateCategoryOrderRequest updateCategoryOrderRequest) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        todoService.updateCategoryOrder(userId, updateCategoryOrderRequest);
        return ApiResponse.success(SuccessCode.OK);
    }

    @Operation(summary = "카테고리 목록 조회", description = "사용자의 모든 카테고리 목록을 순서대로 조회합니다.")
    @GetMapping("/categories")
    public ApiResponse<List<CategoryResponse>> getCategories(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        List<CategoryResponse> categories = todoService.getCategories(userId);
        return ApiResponse.success(SuccessCode.OK, categories);
    }

    @Operation(summary = "카테고리 삭제", description = "카테고리를 삭제합니다. 해당 카테고리의 모든 할 일(Todo)도 함께 삭제됩니다.")
    @DeleteMapping("/categories/{categoryId}")
    public ApiResponse<Void> deleteCategory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID categoryId) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        todoService.deleteCategory(userId, categoryId);
        return ApiResponse.success(SuccessCode.OK);
    }

    /*
     * == Todo (할 일) API ==
     */

    @Operation(summary = "'오늘'의 To-do 목록 조회", description = "오늘 날짜의 모든 카테고리와 할 일 목록을 조회합니다.")
    @GetMapping("/today")
    public ApiResponse<List<CategoryWithTodosResponse>> getTodayTodos(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        List<CategoryWithTodosResponse> todayTodos = todoService.getTodosGroupedByCategory(userId, LocalDate.now());
        return ApiResponse.success(SuccessCode.OK, todayTodos);
    }

    @Operation(summary = "특정 날짜의 To-do 목록 조회", description = "특정 날짜의 모든 카테고리와 할 일 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<List<CategoryWithTodosResponse>> getTodosByDate(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("date") LocalDate date) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        List<CategoryWithTodosResponse> todos = todoService.getTodosGroupedByCategory(userId, date);
        return ApiResponse.success(SuccessCode.OK, todos);
    }

    @Operation(summary = "할 일(Todo) 생성", description = "특정 카테고리에 새로운 할 일을 추가합니다.")
    @PostMapping
    public ApiResponse<TodoResponse> createTodo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateTodoRequest createTodoRequest) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        TodoResponse newTodo = todoService.createTodo(userId, createTodoRequest);
        return ApiResponse.success(SuccessCode.CREATED, newTodo);
    }

    @Operation(summary = "할 일(Todo) 수정", description = "기존 할 일의 내용을 수정합니다.")
    @PutMapping("/{todoId}")
    public ApiResponse<TodoResponse> updateTodo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID todoId,
            @Valid @RequestBody UpdateTodoRequest updateTodoRequest) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        TodoResponse updatedTodo = todoService.updateTodo(userId, todoId, updateTodoRequest);
        return ApiResponse.success(SuccessCode.OK, updatedTodo);
    }

    @Operation(summary = "할 일(Todo) 완료/미완료 토글", description = "할 일의 완료 상태를 토글합니다.")
    @PatchMapping("/{todoId}/complete")
    public ApiResponse<TodoResponse> toggleTodoComplete(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID todoId) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        TodoResponse toggledTodo = todoService.toggleTodoComplete(userId, todoId);
        return ApiResponse.success(SuccessCode.OK, toggledTodo);
    }

    @Operation(summary = "할 일(Todo) 삭제", description = "할 일을 삭제합니다.")
    @DeleteMapping("/{todoId}")
    public ApiResponse<Void> deleteTodo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID todoId) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        todoService.deleteTodo(userId, todoId); // 5. 로직 호출
        return ApiResponse.success(SuccessCode.OK);
    }
}