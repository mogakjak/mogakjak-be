package com.mogakjak.mogakjak.domain.todo.service;

import com.mogakjak.mogakjak.domain.group.service.GroupService;
import com.mogakjak.mogakjak.domain.lounge.service.OfficialLoungeService;
import com.mogakjak.mogakjak.domain.timer.entity.*;
import com.mogakjak.mogakjak.domain.timer.enumerate.*;
import com.mogakjak.mogakjak.domain.timer.repository.*;
import com.mogakjak.mogakjak.domain.timer.service.FocusSessionServiceImpl;
import com.mogakjak.mogakjak.domain.todo.controller.dto.UpdateTodoTargetTimeRequest;
import com.mogakjak.mogakjak.domain.todo.entity.Todo;
import com.mogakjak.mogakjak.domain.todo.repository.TodoRepository;
import com.mogakjak.mogakjak.domain.user.entity.*;
import com.mogakjak.mogakjak.domain.user.repository.*;
import com.mogakjak.mogakjak.global.enumerate.CategoryColor;
import com.mogakjak.mogakjak.global.exception.CustomException;
import com.mogakjak.mogakjak.global.websocket.service.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.*;
import java.time.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(showSql = false)
@Import({TodoSidebarService.class, TodoLastWorkedAtService.class, TodoServiceImpl.class, FocusSessionServiceImpl.class})
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=${ISSUE101_MYSQL_URL}",
        "spring.datasource.username=root", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@EnabledIfEnvironmentVariable(named = "ISSUE101_MYSQL_URL",
        matches = "jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/issue101_verify\\?.+")
class TodoSidebarRepositoryTest {
    @Autowired private TodoSidebarService sidebar;
    @Autowired private TodoServiceImpl todoService;
    @Autowired private FocusSessionServiceImpl timerService;
    @Autowired private UserRepository users;
    @Autowired private CategoryRepository categories;
    @Autowired private TodoRepository todos;
    @Autowired private FocusSessionRepository sessions;
    @Autowired private ActiveFocusSessionRepository active;
    @Autowired private FocusIntervalRepository intervals;
    @Autowired private EntityManager entityManager;
    @MockBean private GroupMemberStatusService memberStatus;
    @MockBean private TimerCompletionNotificationService completion;
    @MockBean private OfficialLoungeService lounge;
    @MockBean private UserActiveStatusService userStatus;
    @MockBean private GroupService groupService;
    private User user;
    private Todo todo;

    @BeforeEach
    void setup() {
        user = users.saveAndFlush(User.create(UUID.randomUUID() + "@example.com", "sidebar-test"));
        var category = categories.saveAndFlush(Category.builder().user(user).name("기본").color(CategoryColor.GREEN)
                .displayOrder(1).build());
        todo = todos.saveAndFlush(Todo.builder().user(user).category(category).task("독서")
                .date(LocalDate.of(2026, 10, 5)).targetTimeInSeconds(3600).actualTimeInSeconds(600).build());
    }

    @ParameterizedTest
    @EnumSource(TimerMode.class)
    void liveSnapshotGoalsAndExistingVisibilityApiStayConsistent(TimerMode mode) {
        FocusSession session = session(mode);
        var start = LocalDateTime.now().minusSeconds(60).withNano(0);
        intervals.saveAndFlush(FocusInterval.create(session.getId(), start,
                mode == TimerMode.POMODORO ? PomodoroPhaseType.FOCUS : PomodoroPhaseType.NORMAL, 1));
        entityManager.clear();
        var response = sidebar.getTodoDetail(user.getId(), todo.getId());
        long elapsed = Duration.between(start,
                response.serverTime().atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()).getSeconds();
        assertEquals(600 + elapsed, response.accumulatedTimeInSeconds());
        assertEquals(elapsed, response.activeSession().focusTimeInSeconds());
        assertEquals(response.serverTime(), response.todo().getLastWorkedAt());
        timerService.updatePersonalTimerVisibility(user, session.getId(), false, false);
        entityManager.flush();
        entityManager.clear();
        assertFalse(sidebar.getTodoDetail(user.getId(), todo.getId()).isTaskPublic());
        assertFalse(sidebar.getTodoDetail(user.getId(), todo.getId()).isTimerPublic());

        UpdateTodoTargetTimeRequest request = new UpdateTodoTargetTimeRequest();
        request.setTargetTimeInSeconds(null);
        todoService.updateTodoTargetTime(user.getId(), todo.getId(), request);
        entityManager.flush();
        entityManager.clear();
        var cleared = sidebar.getTodoDetail(user.getId(), todo.getId());
        assertNull(cleared.todo().getTargetTimeInSeconds());
        assertNull(cleared.progressRate());
        assertEquals(600, cleared.todo().getActualTimeInSeconds());
        request.setTargetTimeInSeconds(1800);
        todoService.updateTodoTargetTime(user.getId(), todo.getId(), request);
        entityManager.flush();
        entityManager.clear();
        var changed = sidebar.getTodoDetail(user.getId(), todo.getId());
        assertEquals(1800, changed.todo().getTargetTimeInSeconds());
        assertEquals((int) Math.floor(changed.accumulatedTimeInSeconds() / 1800.0 * 100), changed.progressRate());
        assertEquals(600, todos.findById(todo.getId()).orElseThrow().getActualTimeInSeconds());
    }

    @Test
    void actualOwnerAndSoftDeletePredicatesRejectAccess() {
        User other = users.saveAndFlush(User.create(UUID.randomUUID() + "@example.com", "other-test"));
        assertThrows(CustomException.class, () -> sidebar.getTodoDetail(other.getId(), todo.getId()));
        todo.softDelete();
        todos.saveAndFlush(todo);
        entityManager.clear();
        assertThrows(CustomException.class, () -> sidebar.getTodoDetail(user.getId(), todo.getId()));
    }

    @Test
    void closedFocusIsCreditedBeforeBreakAndBreakPauseDoesNotAddRest() {
        FocusSession session = session(TimerMode.POMODORO);
        var start = LocalDateTime.now().minusSeconds(61).withNano(0);
        intervals.saveAndFlush(FocusInterval.create(session.getId(), start, PomodoroPhaseType.FOCUS, 1));
        timerService.nextPomodoroPhase(user, session.getId());
        entityManager.flush();
        entityManager.clear();
        int afterFocus = todos.findById(todo.getId()).orElseThrow().getActualTimeInSeconds();
        assertTrue(afterFocus >= 661);
        var breakInterval = intervals.findTopBySessionIdOrderByStartedAtDesc(session.getId()).orElseThrow();
        assertEquals(PomodoroPhaseType.BREAK, breakInterval.getPhaseType());
        var duringBreak = sidebar.getTodoDetail(user.getId(), todo.getId());
        assertEquals(afterFocus, duringBreak.accumulatedTimeInSeconds());
        assertEquals(afterFocus - 600L, duringBreak.activeSession().focusTimeInSeconds());
        timerService.pauseSession(user, session.getId());
        entityManager.flush();
        entityManager.clear();
        var paused = sidebar.getTodoDetail(user.getId(), todo.getId());
        assertEquals(afterFocus, paused.accumulatedTimeInSeconds());
        assertFalse(paused.activeSession().isCounting());
        timerService.finishSession(user, session.getId());
        entityManager.flush();
        entityManager.clear();
        assertEquals(afterFocus, sidebar.getTodoDetail(user.getId(), todo.getId()).accumulatedTimeInSeconds());
        assertNull(sidebar.getTodoDetail(user.getId(), todo.getId()).activeSession());
    }

    private FocusSession session(TimerMode mode) {
        var now = LocalDateTime.now().minusMinutes(10);
        FocusSession session = switch (mode) {
            case TIMER -> FocusSession.createTimerSession(user.getId(), todo, now, 1800L, ParticipationType.INDIVIDUAL, null, true, true);
            case STOPWATCH -> FocusSession.createStopwatchSession(user.getId(), todo, now, ParticipationType.INDIVIDUAL, null, true, true);
            case POMODORO -> FocusSession.createPomodoroSession(user.getId(), todo, now, 60L, 30L, 2, ParticipationType.INDIVIDUAL, null, true, true);
        };
        sessions.saveAndFlush(session);
        active.saveAndFlush(ActiveFocusSession.create(session.getId(), user.getId(), now));
        return session;
    }
}
