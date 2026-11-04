package com.mogakjak.mogakjak.domain.timer.repository;

import com.mogakjak.mogakjak.domain.timer.entity.FocusInterval;
import com.mogakjak.mogakjak.domain.timer.entity.FocusSession;
import com.mogakjak.mogakjak.domain.timer.enumerate.ParticipationType;
import com.mogakjak.mogakjak.domain.timer.enumerate.PomodoroPhaseType;
import com.mogakjak.mogakjak.domain.timer.service.FocusAttendanceService;
import com.mogakjak.mogakjak.domain.user.entity.Category;
import com.mogakjak.mogakjak.domain.todo.entity.Todo;
import com.mogakjak.mogakjak.global.enumerate.CategoryColor;
import com.mogakjak.mogakjak.domain.user.repository.CategoryRepository;
import com.mogakjak.mogakjak.domain.todo.repository.TodoRepository;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.repository.UserRepository;
import com.mogakjak.mogakjak.domain.user.service.MyPageAttendanceService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(showSql = false)
@Import({FocusAttendanceService.class, MyPageAttendanceService.class, FocusAttendanceRepositoryTest.TestClock.class})
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=${MYPAGE_MYSQL_URL}",
        "spring.datasource.username=root", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@EnabledIfEnvironmentVariable(named = "MYPAGE_MYSQL_URL",
        matches = "jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/mypage_verify\\?.+")
class FocusAttendanceRepositoryTest {
    @Autowired private UserRepository users;
    @Autowired private CategoryRepository categories;
    @Autowired private TodoRepository todos;
    @Autowired private FocusSessionRepository sessions;
    @Autowired private FocusIntervalRepository intervals;
    @Autowired private FocusAttendanceService attendance;
    @Autowired private MyPageAttendanceService calendar;
    @Autowired private EntityManager em;
    private User user;
    private Todo todo;
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 19);
    private static final LocalDateTime NOW = TODAY.atTime(12, 0);

    @TestConfiguration
    static class TestClock {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-10-19T03:00:00Z"), ZoneOffset.UTC);
        }
    }

    @BeforeEach
    void setup() {
        user = users.saveAndFlush(User.create(UUID.randomUUID() + "@example.com", "attendance-test"));
        Category category = categories.saveAndFlush(Category.builder().user(user).name("기본")
                .color(CategoryColor.GREEN).displayOrder(1).build());
        todo = todos.saveAndFlush(Todo.builder().user(user).category(category).task("공부").date(TODAY).build());
    }

    @Test
    void queryCountsOnlyOwnCompletedFocusAndNormalIntervals() {
        FocusSession own = session(user, null);
        interval(own, TODAY.atTime(9, 0), TODAY.atTime(9, 15), PomodoroPhaseType.NORMAL);
        interval(own, TODAY.atTime(10, 0), TODAY.atTime(10, 15), PomodoroPhaseType.FOCUS);
        interval(own, TODAY.atTime(10, 15), TODAY.atTime(11, 15), PomodoroPhaseType.BREAK);
        interval(own, TODAY.atTime(11, 15), null, PomodoroPhaseType.NORMAL);
        interval(own, TODAY.atTime(11, 30), TODAY.atTime(11, 30), PomodoroPhaseType.NORMAL);
        interval(own, TODAY.atTime(11, 45), TODAY.atTime(11, 30), PomodoroPhaseType.NORMAL);
        User other = users.saveAndFlush(User.create(UUID.randomUUID() + "@example.com", "other"));
        interval(session(other, null), TODAY.minusDays(1).atTime(9, 0),
                TODAY.minusDays(1).atTime(10, 0), PomodoroPhaseType.NORMAL);
        FocusInterval orphan = FocusInterval.create(UUID.randomUUID(), TODAY.atTime(9, 0),
                PomodoroPhaseType.NORMAL, 1);
        orphan.end(TODAY.atTime(10, 0));
        intervals.saveAndFlush(orphan);
        em.clear();

        var summary = attendance.getSummary(user.getId());

        assertEquals(1, summary.attendanceDays());
        assertEquals(1, summary.dailyFocusSeconds().size());
        assertEquals(1_800L, summary.dailyFocusSeconds().get(TODAY));
    }

    @Test
    void futureIntervalsAreExcludedButAnIntervalEndingExactlyAtSnapshotCounts() {
        FocusSession own = session(user, null);
        interval(own, NOW.minusSeconds(1_799), NOW, PomodoroPhaseType.NORMAL);
        interval(own, NOW.minusHours(1), NOW.plusSeconds(1), PomodoroPhaseType.NORMAL);
        interval(own, TODAY.plusDays(1).atTime(9, 0), TODAY.plusDays(1).atTime(10, 0), PomodoroPhaseType.NORMAL);
        em.clear();

        var response = calendar.getAttendance(user.getId());

        assertEquals(0, response.totalAttendanceDays());
        assertEquals(1_799, response.days().get(5).focusTimeInSeconds());
        assertFalse(response.days().get(5).attended());
        assertTrue(response.days().get(6).future());
        assertEquals(0, response.days().get(6).focusTimeInSeconds());
    }

    @Test
    void calendarSplitsMidnightAndLifetimeCountIncludesHistoryOutsideWindow() {
        FocusSession group = session(user, UUID.randomUUID());
        interval(group, TODAY.minusDays(1).atTime(23, 50), TODAY.atTime(0, 20), PomodoroPhaseType.FOCUS);
        interval(group, TODAY.atTime(1, 0), TODAY.atTime(1, 10), PomodoroPhaseType.NORMAL);
        interval(group, TODAY.minusDays(10).atTime(9, 0), TODAY.minusDays(10).atTime(9, 30), PomodoroPhaseType.NORMAL);
        group.pause(0);
        todo.toggleComplete();
        todo.softDelete();
        em.flush();
        em.clear();

        var response = calendar.getAttendance(user.getId());

        assertEquals(TODAY, response.referenceDate());
        assertEquals(2, response.totalAttendanceDays());
        assertEquals(600, response.days().get(4).focusTimeInSeconds());
        assertFalse(response.days().get(4).attended());
        assertEquals(1_800, response.days().get(5).focusTimeInSeconds());
        assertTrue(response.days().get(5).attended());
        assertEquals(1, response.days().stream().filter(day -> day.attended()).count());
    }

    private FocusSession session(User owner, UUID groupId) {
        return sessions.saveAndFlush(FocusSession.createStopwatchSession(owner.getId(), todo,
                TODAY.minusDays(10).atStartOfDay(), groupId == null ? ParticipationType.INDIVIDUAL : ParticipationType.GROUP,
                groupId, true, true));
    }

    private void interval(FocusSession session, LocalDateTime start, LocalDateTime end, PomodoroPhaseType phase) {
        FocusInterval interval = FocusInterval.create(session.getId(), start, phase, 1);
        interval.end(end);
        intervals.saveAndFlush(interval);
    }
}
