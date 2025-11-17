package com.mogakjak.mogakjak.domain.todo.service;

import com.mogakjak.mogakjak.domain.todo.controller.dto.CategoryResponse;
import com.mogakjak.mogakjak.domain.todo.controller.dto.CategoryWithTodosResponse;
import com.mogakjak.mogakjak.domain.todo.controller.dto.CreateCategoryRequest;
import com.mogakjak.mogakjak.domain.todo.controller.dto.CreateTodoRequest;
import com.mogakjak.mogakjak.domain.todo.entity.Todo;
import com.mogakjak.mogakjak.domain.todo.controller.dto.TodoResponse;
import com.mogakjak.mogakjak.domain.todo.controller.dto.UpdateCategoryOrderRequest;
import com.mogakjak.mogakjak.domain.todo.controller.dto.UpdateTodoRequest;
import com.mogakjak.mogakjak.domain.todo.repository.TodoRepository;
import com.mogakjak.mogakjak.domain.user.entity.Category;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.repository.CategoryRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserRepository;
import com.mogakjak.mogakjak.global.exception.CustomException;
import com.mogakjak.mogakjak.global.exception.status.ErrorCode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class TodoServiceImpl implements TodoService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TodoRepository todoRepository;

    /**
     * 카테고리 생성
     */
    @Override
    public CategoryResponse createCategory(UUID userId, CreateCategoryRequest req) {
        User user = findUserById(userId);

        // 해당 유저의 카테고리 중 가장 높은 displayOrder 값을 찾음
        Integer lastOrder = categoryRepository.findMaxDisplayOrderByUser(user)
                .orElse(0);

        Category category = Category.builder()
                .user(user)
                .name(req.getName())
                .color(req.getColor())
                .displayOrder(lastOrder + 1)
                .isExpanded(true)
                .build();

        Category savedCategory = categoryRepository.save(category);
        return CategoryResponse.from(savedCategory);
    }

    /**
     * 카테고리 순서 변경
     */
    @Override
    public void updateCategoryOrder(UUID userId, UpdateCategoryOrderRequest req) {
        User user = findUserById(userId);

        Map<UUID, Category> categoryMap = categoryRepository.findAllByUserAndIsDeletedFalse(user).stream()
                .collect(Collectors.toMap(Category::getId, c -> c));

        List<UUID> categoryIds = req.getCategoryIds();

        if (req.getCategoryIds().size() != categoryMap.size()) {
            throw new CustomException(ErrorCode.FORBIDDEN_CATEGORY_ACCESS);
        }

        for (UUID id : categoryIds) {
            if (!categoryMap.containsKey(id)) {
                throw new CustomException(ErrorCode.FORBIDDEN_CATEGORY_ACCESS);
            }
        }

        // 순서를 기반으로 displayOrder 업데이트
        IntStream.range(0, categoryIds.size())
                .forEach(index -> {
                    UUID categoryId = categoryIds.get(index);
                    Category category = categoryMap.get(categoryId);
                    category.updateDisplayOrder(index + 1);
                });
    }

    /**
     * 카테고리 목록 조회 (새로 추가)
     */
    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories(UUID userId) {
        User user = findUserById(userId);

        // 유저의 모든 카테고리를 순서(displayOrder)대로 조회
        List<Category> categories = categoryRepository.findAllByUserAndIsDeletedFalseOrderByDisplayOrderAsc(user);

        return categories.stream()
                .map(CategoryResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteCategory(UUID userId, UUID categoryId) {
        User user = findUserById(userId);
        Category category = findCategoryByIdAndUser(categoryId, user);

        category.softDelete();
        category.getTodos().forEach(Todo::softDelete);
    }

    /*
     * == Todo (할 일) API 로직 ==
     */

    /**
     * 특정 날짜의 To-do 목록 조회 (카테고리 그룹화)
     */
    @Override
    @Transactional(readOnly = true)
    public List<CategoryWithTodosResponse> getTodosGroupedByCategory(UUID userId, LocalDate date) {
        User user = findUserById(userId);

        // 1. 유저의 모든 카테고리를 순서대로 조회
        List<Category> categories = categoryRepository.findAllByUserAndIsDeletedFalseOrderByDisplayOrderAsc(user);

        // 2. 유저의 해당 날짜 To-do 목록을 조회
        List<Todo> todos = todoRepository.findAllByUserAndDateAndIsDeletedFalseOrderByCreatedAtAsc(user, date);

        // 3. To-do들을 카테고리 ID별로 그룹화
        Map<UUID, List<TodoResponse>> todosByCategoryId = todos.stream()
                .collect(Collectors.groupingBy(
                        todo -> todo.getCategory().getId(),
                        Collectors.mapping(TodoResponse::from, Collectors.toList())
                ));

        // 4. 카테고리 목록을 순회하며 DTO 조립
        return categories.stream()
                .map(category -> CategoryWithTodosResponse.of(
                        category,
                        todosByCategoryId.getOrDefault(category.getId(), List.of()) // 해당 카테고리의 To-do 목록
                ))
                .collect(Collectors.toList());
    }

    /**
     * 할 일(Todo) 생성
     */
    @Override
    public TodoResponse createTodo(UUID userId, CreateTodoRequest req) {
        User user = findUserById(userId);
        Category category = findCategoryByIdAndUser(req.getCategoryId(), user);

        Todo todo = Todo.builder()
                .user(user)
                .category(category)
                .task(req.getTask())
                .date(req.getDate())
                .targetTimeInSeconds(req.getTargetTimeInSeconds())
                .build(); // actualTime, isCompleted는 default 값 사용

        Todo savedTodo = todoRepository.save(todo);
        return TodoResponse.from(savedTodo);
    }

    /**
     * 할 일(Todo) 수정
     */
    @Override
    public TodoResponse updateTodo(UUID userId, UUID todoId, UpdateTodoRequest req) {
        User user = findUserById(userId);
        Todo todo = findTodoByIdAndUser(todoId, user);

        // 카테고리가 변경되었는지 확인
        Category category = todo.getCategory();
        if (!category.getId().equals(req.getCategoryId())) {
            category = findCategoryByIdAndUser(req.getCategoryId(), user);
        }

        todo.updateInfo(
                category,
                req.getTask(),
                req.getDate(),
                req.getTargetTimeInSeconds()
        );

        return TodoResponse.from(todo);
    }

    /**
     * 할 일(Todo) 완료/미완료 토글
     */
    @Override
    public TodoResponse toggleTodoComplete(UUID userId, UUID todoId) {
        User user = findUserById(userId);
        Todo todo = findTodoByIdAndUser(todoId, user);

        todo.toggleComplete();

        return TodoResponse.from(todo);
    }

    /**
     * 할 일(Todo) 삭제
     */
    @Override
    public void deleteTodo(UUID userId, UUID todoId) {
        User user = findUserById(userId);
        Todo todo = findTodoByIdAndUser(todoId, user);

        todo.softDelete();
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private Category findCategoryByIdAndUser(UUID categoryId, User user) {
        return categoryRepository.findByIdAndUserAndIsDeletedFalse(categoryId, user)
                .orElseThrow(() -> new CustomException(ErrorCode.FORBIDDEN_CATEGORY_ACCESS));
    }

    private Todo findTodoByIdAndUser(UUID todoId, User user) {
        return todoRepository.findByIdAndUserAndIsDeletedFalse(todoId, user)
                .orElseThrow(() -> new CustomException(ErrorCode.FORBIDDEN_TODO_ACCESS));
    }
}