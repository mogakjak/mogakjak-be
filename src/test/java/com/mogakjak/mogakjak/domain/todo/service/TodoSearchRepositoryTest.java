package com.mogakjak.mogakjak.domain.todo.service;

import com.mogakjak.mogakjak.domain.todo.controller.dto.TodoSearchRequest;
import com.mogakjak.mogakjak.domain.todo.controller.dto.UpdateTodoRequest;
import com.mogakjak.mogakjak.domain.todo.entity.Todo;
import com.mogakjak.mogakjak.domain.todo.repository.TodoRepository;
import com.mogakjak.mogakjak.domain.user.entity.Category;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.repository.CategoryRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserRepository;
import com.mogakjak.mogakjak.domain.timer.entity.FocusSession;
import com.mogakjak.mogakjak.domain.timer.entity.FocusInterval;
import com.mogakjak.mogakjak.domain.timer.enumerate.ParticipationType;
import com.mogakjak.mogakjak.domain.timer.enumerate.PomodoroPhaseType;
import com.mogakjak.mogakjak.domain.timer.repository.FocusSessionRepository;
import com.mogakjak.mogakjak.domain.timer.repository.FocusIntervalRepository;
import com.mogakjak.mogakjak.global.enumerate.CategoryColor;
import com.mogakjak.mogakjak.global.exception.CustomException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(showSql = false)
@Import({TodoSearchService.class, TodoLastWorkedAtService.class, TodoServiceImpl.class})
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=${ISSUE102_MYSQL_URL}",
        "spring.datasource.username=root", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.properties.hibernate.generate_statistics=true"
})
@EnabledIfEnvironmentVariable(named = "ISSUE102_MYSQL_URL",
        matches = "jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/issue102_verify\\?.+")
class TodoSearchRepositoryTest {
    @Autowired private TodoSearchService search;
    @Autowired private TodoServiceImpl todoService;
    @Autowired private UserRepository users;
    @Autowired private CategoryRepository categories;
    @Autowired private TodoRepository todos;
    @Autowired private FocusSessionRepository sessions;
    @Autowired private FocusIntervalRepository intervals;
    @Autowired private EntityManager em;
    private User user;
    private Category category;

    @BeforeEach
    void setup() {
        user = users.saveAndFlush(User.create(UUID.randomUUID() + "@example.com", "search-test"));
        category = category(user);
    }

    @Test
    void filtersOwnershipAndDeletedRecordsButIncludesCompletedOldTasks() {
        var kept = todo(user, category, "가방");
        kept.toggleComplete();
        var deleted = todo(user, category, "가방 삭제");
        deleted.softDelete();
        var other = users.saveAndFlush(User.create(UUID.randomUUID() + "@example.com", "other"));
        todo(other, category(other), "가방 타인");
        var removedCategory = category(user);
        removedCategory.softDelete();
        todo(user, removedCategory, "가방 삭제카테고리");
        todo(user, category(other), "가방 잘못된소유권");
        em.flush();
        em.clear();
        var response = search.search(user.getId(), request("ㄱㅂ", 20, null));
        assertEquals(1, response.items().size());
        assertEquals(kept.getId(), response.items().getFirst().todo().getId());
        assertTrue(response.items().getFirst().todo().getIsCompleted());
        assertEquals(LocalDate.of(2020, 1, 1), response.items().getFirst().todo().getDate());
        assertNull(response.items().getFirst().todo().getTargetTimeInSeconds());
        assertNull(response.items().getFirst().todo().getLastWorkedAt());
        assertEquals(category.getId(), response.items().getFirst().category().getId());
    }

    @Test
    void completeMixedAndSpecialCharacterQueriesUseLiteralMatching() {
        var bag = todo(user, category, "가방");
        todo(user, category, "각본");
        var percent = todo(user, category, "100% 완료");
        var underscore = todo(user, category, "a_b");
        var regex = todo(user, category, ".* 그대로");
        em.flush();
        em.clear();
        for (String keyword : new String[]{"가", "가ㅂ", "가", "ㄱㅏ"}) {
            assertEquals(bag.getId(), search.search(user.getId(), request(keyword, 20, null))
                    .items().getFirst().todo().getId());
            assertEquals(1, search.search(user.getId(), request(keyword, 20, null)).items().size());
        }
        assertEquals(2, search.search(user.getId(), request("ㄱㅂ", 20, null)).items().size());
        assertEquals(percent.getId(), search.search(user.getId(), request("%", 20, null)).items().getFirst().todo().getId());
        assertEquals(underscore.getId(), search.search(user.getId(), request("_", 20, null)).items().getFirst().todo().getId());
        assertEquals(regex.getId(), search.search(user.getId(), request(".*", 20, null)).items().getFirst().todo().getId());
    }

    @Test
    void cursorOrdersEqualTimestampsByBinaryUuidWithoutDuplicatesOrOmissions() {
        var expected = new ArrayList<UUID>();
        for (int i = 0; i < 7; i++) {
            var item = todo(user, category, "가방 " + i);
            setCreated(item, LocalDateTime.of(2026, 10, 1, 12, 0));
            expected.add(item.getId());
        }
        expected.sort(Comparator.comparing(UUID::toString).reversed());
        em.clear();
        var received = new ArrayList<UUID>();
        UUID cursor = null;
        do {
            var response = search.search(user.getId(), request("ㄱㅂ", 2, cursor));
            received.addAll(response.items().stream().map(item -> item.todo().getId()).toList());
            assertEquals(response.hasNext(), response.nextCursor() != null);
            cursor = response.nextCursor();
        } while (cursor != null);
        assertEquals(expected, received);
        assertEquals(expected, search.search(user.getId(), request("  ", 100, null))
                .items().stream().map(item -> item.todo().getId()).toList());
    }

    @Test
    void scansBeyondFirstCandidateBatchAndReturnsEmptyAtEnd() {
        for (int i = 0; i < 260; i++) todo(user, category, "나무 " + i);
        var older = todo(user, category, "가방");
        setCreated(older, LocalDateTime.of(2020, 1, 1, 0, 0));
        em.clear();
        var result = search.search(user.getId(), request("ㄱㅂ", 1, null));
        assertEquals(older.getId(), result.items().getFirst().todo().getId());
        assertFalse(result.hasNext());
        assertTrue(search.search(user.getId(), request("없는제목", 20, null)).items().isEmpty());
    }

    @Test
    void titleUpdateImmediatelyChangesInitialAndLiteralSearch() {
        var item = todo(user, category, "가방");
        todoService.updateTodo(user.getId(), item.getId(),
                new UpdateTodoRequest(category.getId(), "나무", item.getDate(), null));
        em.flush();
        em.clear();
        assertTrue(search.search(user.getId(), request("ㄱ", 20, null)).items().isEmpty());
        assertEquals(item.getId(), search.search(user.getId(), request("ㄴㅁ", 20, null)).items().getFirst().todo().getId());
        assertEquals(item.getId(), search.search(user.getId(), request("나무", 20, null)).items().getFirst().todo().getId());
    }

    @Test
    void foreignDeletedAndDeletedCategoryCursorsAreRejected() {
        var other = users.saveAndFlush(User.create(UUID.randomUUID() + "@example.com", "other"));
        var foreign = todo(other, category(other), "가방");
        var deleted = todo(user, category, "가방");
        deleted.softDelete();
        var removedCategory = category(user);
        var removed = todo(user, removedCategory, "가방");
        removedCategory.softDelete();
        em.flush();
        em.clear();
        for (UUID cursor : new UUID[]{foreign.getId(), deleted.getId(), removed.getId(), UUID.randomUUID()}) {
            assertThrows(CustomException.class, () -> search.search(user.getId(), request("", 20, cursor)));
        }
    }

    @Test
    void categoryAndHistoryQueriesDoNotGrowWithResultCountAndIndexExists() {
        Todo worked = null;
        for (int i = 0; i < 8; i++) worked = todo(user, category(user), "가방 " + i);
        var ended = LocalDateTime.of(2026, 10, 4, 12, 0);
        var session = sessions.saveAndFlush(FocusSession.createStopwatchSession(user.getId(), worked,
                ended.minusMinutes(1), ParticipationType.INDIVIDUAL, null, true, true));
        session.end(ended, 0);
        var interval = FocusInterval.create(session.getId(), ended.minusMinutes(1), PomodoroPhaseType.NORMAL, 1);
        interval.end(ended);
        intervals.saveAndFlush(interval);
        em.flush();
        em.clear();
        var statistics = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        var result = search.search(user.getId(), request("ㄱㅂ", 20, null));
        long statements = statistics.getPrepareStatementCount();
        assertEquals(8, result.items().size());
        var workedId = worked.getId();
        assertEquals(ended.atOffset(java.time.ZoneOffset.ofHours(9)), result.items().stream()
                .filter(item -> item.todo().getId().equals(workedId)).findFirst().orElseThrow().todo().getLastWorkedAt());
        assertTrue(statements <= 6, "expected bounded owner/candidate/category/history queries, got " + statements);
        Number indexes = (Number) em.createNativeQuery("""
                SELECT COUNT(*) FROM information_schema.statistics
                WHERE table_schema = DATABASE() AND table_name = 'todo'
                  AND index_name = 'idx_todo_user_deleted_created'
                """).getSingleResult();
        assertEquals(4, indexes.intValue());
    }

    private Category category(User owner) {
        return categories.saveAndFlush(Category.builder().user(owner).name("기본").color(CategoryColor.GREEN)
                .displayOrder(1).build());
    }

    private Todo todo(User owner, Category group, String task) {
        return todos.saveAndFlush(Todo.builder().user(owner).category(group).task(task)
                .date(LocalDate.of(2020, 1, 1)).build());
    }

    private void setCreated(Todo item, LocalDateTime date) {
        em.createNativeQuery("UPDATE todo SET created_at = :date WHERE id = :id")
                .setParameter("date", date).setParameter("id", item.getId()).executeUpdate();
    }

    private TodoSearchRequest request(String keyword, int limit, UUID cursor) {
        var request = new TodoSearchRequest();
        request.setKeyword(keyword);
        request.setLimit(limit);
        request.setCursor(cursor);
        return request;
    }
}
