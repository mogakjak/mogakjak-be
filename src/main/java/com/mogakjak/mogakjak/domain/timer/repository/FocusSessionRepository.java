package com.mogakjak.mogakjak.domain.timer.repository;

import com.mogakjak.mogakjak.domain.timer.entity.FocusSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FocusSessionRepository extends JpaRepository<FocusSession, UUID> {

    @Query(value = """
    SELECT 
        c.id AS categoryId,
        c.name AS categoryName,
        c.color AS color,
        COALESCE(SUM(s.total_duration), 0) AS totalSeconds,
        (SELECT COUNT(*) FROM todo t WHERE t.category_id = c.id AND t.is_completed = 1 AND t.is_deleted = 0) AS completedTodoCount,
        (SELECT COUNT(*) FROM todo t WHERE t.category_id = c.id AND t.is_deleted = 0) AS totalTodoCount
    FROM category c
    LEFT JOIN focus_session s
        ON s.category_id = c.id
        AND s.user_id = :userId
        AND s.started_at BETWEEN :start AND :end
    GROUP BY c.id, c.name, c.color
    ORDER BY totalSeconds DESC
    """,
            nativeQuery = true)
    List<Object[]> findCategoryStats(
            @Param("userId") UUID userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
    SELECT COALESCE(SUM(s.totalDuration), 0)
    FROM FocusSession s
    WHERE s.userId = :userId
      AND s.participationType = 'INDIVIDUAL'
      AND s.startedAt BETWEEN :start AND :end
    """)
    Long sumPersonalSeconds(
            @Param("userId") UUID userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
    SELECT COALESCE(SUM(s.totalDuration), 0)
    FROM FocusSession s
    WHERE s.userId = :userId
      AND s.participationType = 'GROUP'
      AND s.startedAt BETWEEN :start AND :end
    """)
    Long sumGroupSeconds(
            @Param("userId") UUID userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
    SELECT COALESCE(SUM(s.totalDuration), 0)
    FROM FocusSession s
    WHERE s.userId = :userId
      AND s.startedAt BETWEEN :start AND :end
    """)
    Long sumTotalSeconds(
            @Param("userId") UUID userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    Optional<FocusSession> findTopByUserIdOrderByStartedAtDesc(UUID userId);

    List<FocusSession> findAllByUserIdInOrderByUserIdAscStartedAtDesc(Collection<UUID> userIds);

}
