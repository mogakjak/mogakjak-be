package com.mogakjak.mogakjak.domain.timer.repository;

import com.mogakjak.mogakjak.domain.timer.entity.ActiveFocusSession;
import com.mogakjak.mogakjak.domain.timer.entity.FocusInterval;
import com.mogakjak.mogakjak.domain.timer.entity.FocusSession;
import com.mogakjak.mogakjak.domain.timer.enumerate.ParticipationType;
import com.mogakjak.mogakjak.domain.timer.enumerate.PomodoroPhaseType;
import com.mogakjak.mogakjak.domain.timer.enumerate.TimerStatus;
import com.mogakjak.mogakjak.domain.todo.controller.dto.UpdateTodoRequest;
import com.mogakjak.mogakjak.domain.todo.controller.dto.UpdateTodoTargetTimeRequest;
import com.mogakjak.mogakjak.domain.todo.entity.Todo;
import com.mogakjak.mogakjak.domain.todo.repository.TodoRepository;
import com.mogakjak.mogakjak.domain.todo.service.TodoLastWorkedAtService;
import com.mogakjak.mogakjak.domain.todo.service.TodoServiceImpl;
import com.mogakjak.mogakjak.domain.user.entity.Category;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.repository.CategoryRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserRepository;
import com.mogakjak.mogakjak.global.enumerate.CategoryColor;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(showSql = false)
@Import({TodoServiceImpl.class, TodoLastWorkedAtService.class})
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=${ISSUE100_MYSQL_URL}",
        "spring.datasource.username=root",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@EnabledIfEnvironmentVariable(named = "ISSUE100_MYSQL_URL",
        matches = "jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/issue100_verify\\?.+")
class TodoLastWorkedAtRepositoryTest {
    @Autowired private FocusIntervalRepository intervals;
    @Autowired private FocusSessionRepository sessions;
    @Autowired private ActiveFocusSessionRepository activeSessions;
    @Autowired private TodoRepository todos;
    @Autowired private UserRepository users;
    @Autowired private CategoryRepository categories;
    @Autowired private TodoLastWorkedAtService lastWorkedAt;
    @Autowired private TodoServiceImpl service;
    @Autowired private EntityManager entityManager;

    private User user;
    private Category category;
    private Todo todo;
    private final LocalDateTime now = LocalDateTime.now().withNano(0);
    private final List<PomodoroPhaseType> phases = List.of(PomodoroPhaseType.NORMAL, PomodoroPhaseType.FOCUS);

    @BeforeEach
    void setup() {
        user = users.saveAndFlush(User.create(UUID.randomUUID() + "@example.com", "last-work-test"));
        category = categories.saveAndFlush(Category.builder().user(user).name("기본")
                .color(CategoryColor.GREEN).displayOrder(1).build());
        todo = todo("같은 이름");
    }

    @Test
    void lookupIndexesAreCreatedInMysql() {
        var indexes = entityManager.createNativeQuery("""
                SELECT DISTINCT index_name FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND index_name IN ('idx_focus_session_user_todo',
                                     'idx_focus_interval_session_started',
                                     'idx_focus_interval_session_phase_end')
                """).getResultList();
        assertEquals(3, indexes.size());
    }

    @Test
    void oldIndividualAndGroupPersonalHistoryIsAggregatedByTodoNotNameOrUser() {
        Todo another = todo("같은 이름");
        FocusSession first = session(todo, ParticipationType.INDIVIDUAL);
        interval(first, now.minusDays(10), now.minusDays(10).plusHours(1), PomodoroPhaseType.NORMAL);
        FocusSession group = session(todo, ParticipationType.GROUP);
        interval(group, now.minusDays(3), now.minusDays(3).plusMinutes(10), PomodoroPhaseType.FOCUS);
        interval(group, now.minusDays(1), now.minusDays(1).plusMinutes(10), PomodoroPhaseType.BREAK);
        FocusSession otherTodo = session(another, ParticipationType.INDIVIDUAL);
        interval(otherTodo, now.minusDays(8), now.minusDays(8).plusMinutes(5), PomodoroPhaseType.NORMAL);
        FocusSession otherUser = session(todo, ParticipationType.INDIVIDUAL);
        ReflectionTestUtils.setField(otherUser, "userId", UUID.randomUUID());
        sessions.saveAndFlush(otherUser);
        interval(otherUser, now.minusHours(1), now, PomodoroPhaseType.NORMAL);
        entityManager.clear();

        var result = lastWorkedAt.getLastWorkedAt(user.getId(), List.of(todo.getId(), another.getId()));
        assertEquals(now.minusDays(3).plusMinutes(10), result.get(todo.getId()));
        assertEquals(now.minusDays(8).plusMinutes(5), result.get(another.getId()));
    }

    @Test
    void noHistoryAndZeroNegativeFutureOrBreakOnlyIntervalsAreIgnored() {
        FocusSession session = session(todo, ParticipationType.INDIVIDUAL);
        interval(session, now.minusHours(1), now.minusHours(1), PomodoroPhaseType.NORMAL);
        interval(session, now.minusHours(1), now.minusHours(2), PomodoroPhaseType.FOCUS);
        interval(session, now.minusHours(1), now.plusHours(1), PomodoroPhaseType.NORMAL);
        interval(session, now.minusHours(1), now, PomodoroPhaseType.BREAK);
        Todo none = todo("미작업");
        assertTrue(lastWorkedAt.getLastWorkedAt(user.getId(), List.of(todo.getId(), none.getId())).isEmpty());
    }

    @ParameterizedTest
    @EnumSource(value = PomodoroPhaseType.class, names = {"NORMAL", "FOCUS"})
    void runningLatestFocusUsesQueryTime(PomodoroPhaseType phase) {
        FocusSession session = session(todo, ParticipationType.GROUP);
        interval(session, now.minusMinutes(1), null, phase);
        activate(session);
        assertEquals(List.of(todo.getId()), intervals.findCurrentlyWorkingTodoIds(
                user.getId(), List.of(todo.getId()), phases, TimerStatus.RUNNING, now));
        LocalDateTime before = LocalDateTime.now();
        LocalDateTime result = lastWorkedAt.getLastWorkedAt(user.getId(), List.of(todo.getId())).get(todo.getId());
        assertFalse(result.isBefore(before));
        assertFalse(result.isAfter(LocalDateTime.now()));
    }

    @Test
    void startingAtOrAfterQueryTimeDoesNotCountAsActualWork() {
        FocusSession session = session(todo, ParticipationType.INDIVIDUAL);
        interval(session, now, null, PomodoroPhaseType.NORMAL);
        activate(session);
        assertTrue(intervals.findCurrentlyWorkingTodoIds(
                user.getId(), List.of(todo.getId()), phases, TimerStatus.RUNNING, now).isEmpty());
    }

    @ParameterizedTest
    @EnumSource(value = TimerStatus.class, names = {"PAUSED", "FINISHED"})
    void pausedAndFinishedUseLastClosedFocusEvenWithStaleOpenInterval(TimerStatus status) {
        FocusSession session = session(todo, ParticipationType.INDIVIDUAL);
        LocalDateTime end = now.minusHours(1);
        interval(session, end.minusMinutes(10), end, PomodoroPhaseType.NORMAL);
        interval(session, now.minusMinutes(1), null, PomodoroPhaseType.FOCUS);
        if (status == TimerStatus.PAUSED) session.pause(0);
        else session.end(now, 0);
        sessions.saveAndFlush(session);
        activate(session);
        assertEquals(end, lastWorkedAt.getLastWorkedAt(user.getId(), List.of(todo.getId())).get(todo.getId()));
    }

    @Test
    void orphanOpenFocusIsNotCurrentWork() {
        FocusSession session = session(todo, ParticipationType.INDIVIDUAL);
        interval(session, now.minusMinutes(1), null, PomodoroPhaseType.NORMAL);
        assertTrue(lastWorkedAt.getLastWorkedAt(user.getId(), List.of(todo.getId())).isEmpty());
    }

    @Test
    void breakAndOlderUnclosedFocusDoNotOverrideCompletedWork() {
        FocusSession session = session(todo, ParticipationType.INDIVIDUAL);
        LocalDateTime end = now.minusHours(1);
        interval(session, end.minusMinutes(10), end, PomodoroPhaseType.FOCUS);
        interval(session, now.minusMinutes(10), null, PomodoroPhaseType.FOCUS);
        interval(session, now.minusMinutes(5), null, PomodoroPhaseType.BREAK);
        activate(session);
        assertEquals(end, lastWorkedAt.getLastWorkedAt(user.getId(), List.of(todo.getId())).get(todo.getId()));
    }

    @Test
    void mismatchedActiveUserDoesNotMakeSessionCurrent() {
        FocusSession session = session(todo, ParticipationType.INDIVIDUAL);
        interval(session, now.minusMinutes(1), null, PomodoroPhaseType.NORMAL);
        activeSessions.saveAndFlush(ActiveFocusSession.create(session.getId(), UUID.randomUUID(), now));
        assertTrue(lastWorkedAt.getLastWorkedAt(user.getId(), List.of(todo.getId())).isEmpty());
    }

    @Test
    void listGroupedListAndMutationResponsesPreserveHistoryAndOrder() {
        Todo none = todo("미작업");
        FocusSession session = session(todo, ParticipationType.INDIVIDUAL);
        LocalDateTime end = now.minusDays(2);
        interval(session, end.minusMinutes(10), end, PomodoroPhaseType.NORMAL);
        entityManager.clear();
        var expected = end.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("Asia/Seoul")).toOffsetDateTime();
        var list = service.getUserTodos(user);
        var byId = list.stream().collect(Collectors.toMap(response -> response.getId(), response -> response));
        assertEquals(expected, byId.get(todo.getId()).getLastWorkedAt());
        assertNull(byId.get(none.getId()).getLastWorkedAt());
        var grouped = service.getTodosGroupedByCategory(user.getId(), todo.getDate()).getFirst().getTodos();
        assertEquals(todos.findAllByUserAndDateAndIsDeletedFalseOrderByCreatedAtAsc(user, todo.getDate())
                .stream().map(Todo::getId).toList(), grouped.stream().map(response -> response.getId()).toList());
        assertEquals(expected, grouped.stream().filter(response -> response.getId().equals(todo.getId()))
                .findFirst().orElseThrow().getLastWorkedAt());
        assertEquals(expected, service.updateTodo(user.getId(), todo.getId(),
                new UpdateTodoRequest(category.getId(), "이름만 수정", todo.getDate(), 7200)).getLastWorkedAt());
        UpdateTodoTargetTimeRequest target = new UpdateTodoTargetTimeRequest();
        target.setTargetTimeInSeconds(null);
        assertEquals(expected, service.updateTodoTargetTime(user.getId(), todo.getId(), target).getLastWorkedAt());
        assertEquals(expected, service.toggleTodoComplete(user.getId(), todo.getId()).getLastWorkedAt());
    }

    private Todo todo(String task) {
        return todos.saveAndFlush(Todo.builder().user(user).category(category).task(task)
                .date(LocalDate.of(2026, 10, 5)).targetTimeInSeconds(3600).build());
    }

    private FocusSession session(Todo target, ParticipationType participation) {
        return sessions.saveAndFlush(FocusSession.createStopwatchSession(user.getId(), target, now.minusDays(20),
                participation, participation == ParticipationType.GROUP ? UUID.randomUUID() : null, true, true));
    }

    private void interval(FocusSession session, LocalDateTime start, LocalDateTime end, PomodoroPhaseType phase) {
        FocusInterval interval = FocusInterval.create(session.getId(), start, phase, 1);
        if (end != null) interval.end(end);
        intervals.saveAndFlush(interval);
    }

    private void activate(FocusSession session) {
        activeSessions.saveAndFlush(ActiveFocusSession.create(session.getId(), user.getId(), now));
    }
}
