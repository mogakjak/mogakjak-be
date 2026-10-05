package com.mogakjak.mogakjak.domain.timer.repository;

import com.mogakjak.mogakjak.domain.timer.entity.FocusSession;
import com.mogakjak.mogakjak.domain.timer.entity.ActiveFocusSession;
import com.mogakjak.mogakjak.domain.timer.enumerate.ParticipationType;
import com.mogakjak.mogakjak.domain.timer.enumerate.TimerStatus;
import com.mogakjak.mogakjak.domain.todo.entity.Todo;
import com.mogakjak.mogakjak.domain.todo.controller.dto.TodoResponse;
import com.mogakjak.mogakjak.domain.todo.controller.dto.UpdateTodoTargetTimeRequest;
import com.mogakjak.mogakjak.domain.todo.repository.TodoRepository;
import com.mogakjak.mogakjak.domain.todo.service.TodoServiceImpl;
import com.mogakjak.mogakjak.domain.todo.service.TodoLastWorkedAtService;
import com.mogakjak.mogakjak.domain.user.entity.Category;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.repository.CategoryRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserRepository;
import com.mogakjak.mogakjak.global.enumerate.CategoryColor;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(showSql = false)
@Import({TodoServiceImpl.class, TodoLastWorkedAtService.class})
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=${ISSUE99_MYSQL_URL}",
        "spring.datasource.username=root",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@EnabledIfEnvironmentVariable(named = "ISSUE99_MYSQL_URL",
        matches = "jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/issue99_verify\\?.+")
class FocusSessionTargetTimeRepositoryTest {

    @Autowired private FocusSessionRepository repository;
    @Autowired private EntityManager entityManager;
    @Autowired private TodoRepository todoRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ActiveFocusSessionRepository activeFocusSessionRepository;
    @Autowired private TodoServiceImpl service;
    private final UUID userId = UUID.randomUUID();
    private final UUID todoId = UUID.randomUUID();
    private final List<TimerStatus> activeStatuses = List.of(TimerStatus.RUNNING, TimerStatus.PAUSED);

    @Test
    void servicePersistsGoalChangesAndClearForReselection() {
        User user = userRepository.saveAndFlush(User.create(UUID.randomUUID() + "@example.com", "test-user"));
        Category category = categoryRepository.saveAndFlush(Category.builder().user(user).name("기본")
                .color(CategoryColor.GREEN).displayOrder(1).build());
        Todo todo = todoRepository.saveAndFlush(Todo.builder().user(user).category(category).task("독서")
                .date(LocalDate.of(2026, 10, 5)).targetTimeInSeconds(3600).actualTimeInSeconds(900).build());
        UUID persistedUserId = user.getId();
        UUID persistedTodoId = todo.getId();
        FocusSession session = repository.saveAndFlush(FocusSession.createTimerSession(persistedUserId, todo,
                LocalDateTime.now().withNano(0), 1800L, ParticipationType.INDIVIDUAL, null, true, true));
        session.addDuration(300L);
        session.setProgressRate(25);
        activeFocusSessionRepository.saveAndFlush(ActiveFocusSession.create(session.getId(), persistedUserId, LocalDateTime.now()));
        UUID sessionId = session.getId();
        entityManager.clear();

        for (Integer target : new Integer[]{3600, 7200, null}) {
            UpdateTodoTargetTimeRequest request = new UpdateTodoTargetTimeRequest();
            request.setTargetTimeInSeconds(target);
            service.updateTodoTargetTime(persistedUserId, persistedTodoId, request);
            entityManager.flush();
            entityManager.clear();
            TodoResponse selected = service.getUserTodos(user).getFirst();
            assertEquals(target, selected.getTargetTimeInSeconds());
            Integer expectedRate = target == null ? null : (int) Math.floor(900.0 / target * 100);
            assertEquals(expectedRate, selected.getProgressRate());
            FocusSession updated = repository.findById(sessionId).orElseThrow();
            assertEquals(expectedRate, updated.getProgressRate());
            assertEquals(TimerStatus.RUNNING, updated.getStatus());
            assertEquals(1800L, updated.getTargetDuration());
            assertEquals(300L, updated.getTotalDuration());
            assertEquals(900, selected.getActualTimeInSeconds());
            assertEquals("독서", selected.getTask());
            assertEquals(LocalDate.of(2026, 10, 5), selected.getDate());
        }
    }

    @ParameterizedTest
    @EnumSource(value = TimerStatus.class, names = {"RUNNING", "PAUSED"})
    void updatesOnlyProgressWithoutOverwritingSessionFields(TimerStatus status) {
        FocusSession session = persistSession(status);
        LocalDateTime startedAt = session.getStartedAt();

        assertEquals(1, repository.updateActiveTodoProgressRate(session.getId(), userId, todoId, 12, activeStatuses));
        entityManager.clear();
        FocusSession updated = repository.findById(session.getId()).orElseThrow();
        assertEquals(12, updated.getProgressRate());
        assertEquals(status, updated.getStatus());
        assertEquals(300L, updated.getTotalDuration());
        assertEquals(1800L, updated.getTargetDuration());
        assertEquals(startedAt, updated.getStartedAt());
    }

    @Test
    void clearingTargetPersistsNullProgress() {
        FocusSession session = persistSession(TimerStatus.PAUSED);
        assertEquals(1, repository.updateActiveTodoProgressRate(session.getId(), userId, todoId, null, activeStatuses));
        entityManager.clear();
        assertNull(repository.findById(session.getId()).orElseThrow().getProgressRate());
    }

    @Test
    void checksOwnerAndTodoAtWriteTime() {
        FocusSession session = persistSession(TimerStatus.RUNNING);
        assertEquals(0, repository.updateActiveTodoProgressRate(session.getId(), UUID.randomUUID(), todoId, 12, activeStatuses));
        assertEquals(0, repository.updateActiveTodoProgressRate(session.getId(), userId, UUID.randomUUID(), 12, activeStatuses));
        entityManager.clear();
        assertEquals(25, repository.findById(session.getId()).orElseThrow().getProgressRate());
    }

    @Test
    void sessionFinishedAfterLookupIsNotOverwritten() {
        FocusSession session = persistSession(TimerStatus.RUNNING);
        session.end(LocalDateTime.now(), 30);
        entityManager.flush();

        assertEquals(0, repository.updateActiveTodoProgressRate(session.getId(), userId, todoId, 12, activeStatuses));
        entityManager.clear();
        FocusSession finished = repository.findById(session.getId()).orElseThrow();
        assertEquals(TimerStatus.FINISHED, finished.getStatus());
        assertEquals(30, finished.getProgressRate());
    }

    private FocusSession persistSession(TimerStatus status) {
        Category category = Category.builder().build();
        ReflectionTestUtils.setField(category, "id", UUID.randomUUID());
        Todo todo = Todo.builder().category(category).build();
        ReflectionTestUtils.setField(todo, "id", todoId);
        // Match MySQL's microsecond timestamp precision for the preservation assertion.
        LocalDateTime now = LocalDateTime.now().withNano(0);
        FocusSession session = FocusSession.createTimerSession(userId, todo, now, 1800L,
                ParticipationType.INDIVIDUAL, null, true, true);
        session.addDuration(300L);
        session.setProgressRate(25);
        if (status == TimerStatus.PAUSED) session.pause(25);
        return repository.saveAndFlush(session);
    }
}
