package com.mogakjak.mogakjak.domain.todo.service;

import com.mogakjak.mogakjak.domain.todo.controller.dto.*;
import com.mogakjak.mogakjak.domain.user.entity.User;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TodoService {

    /**
     * 카테고리 생성
     */
    CategoryResponse createCategory(UUID userId, CreateCategoryRequest req);

    /**
     * 카테고리 수정
     */
    CategoryResponse modifyCategory(UUID userId, @Valid UpdateCategoryRequest updateCategoryRequest);

    /**
     * 카테고리 순서 변경
     */
    void updateCategoryOrder(UUID userId, UpdateCategoryOrderRequest req);

    /**
     * 카테고리 목록 조회
     */
    List<CategoryResponse> getCategories(UUID userId);

    /**
     * 카테고리 삭제
     */
    void deleteCategory(UUID userId, UUID categoryId);

    /**
     * 특정 날짜의 To-do 목록 조회 (카테고리 그룹화)
     */
    List<CategoryWithTodosResponse> getTodosGroupedByCategory(UUID userId, LocalDate date);

    /**
     * 할 일(Todo) 생성
     */
    TodoResponse createTodo(UUID userId, CreateTodoRequest req);

    /**
     * 할 일(Todo) 수정
     */
    TodoResponse updateTodo(UUID userId, UUID todoId, UpdateTodoRequest req);

    /**
     * 할 일(Todo) 완료/미완료 토글
     */
    TodoResponse toggleTodoComplete(UUID userId, UUID todoId);

    /**
     * 할 일(Todo) 삭제
     */
    void deleteTodo(UUID userId, UUID todoId);

    /**
     * 로그인 한 유저의 전체 할 일 목록 조회
     */
    List<TodoResponse> getUserTodos(User user);
}