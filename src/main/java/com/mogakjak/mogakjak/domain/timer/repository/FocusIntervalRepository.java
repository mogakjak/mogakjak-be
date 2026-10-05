package com.mogakjak.mogakjak.domain.timer.repository;

import com.mogakjak.mogakjak.domain.timer.entity.FocusInterval;
import com.mogakjak.mogakjak.domain.timer.enumerate.PomodoroPhaseType;
import com.mogakjak.mogakjak.domain.timer.enumerate.TimerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FocusIntervalRepository extends JpaRepository<FocusInterval, UUID> {

    @Query("""
        SELECT s.todoId AS todoId, MAX(i.endedAt) AS lastWorkedAt
        FROM FocusInterval i JOIN FocusSession s ON i.sessionId = s.id
        WHERE s.userId = :userId AND s.todoId IN :todoIds
          AND i.phaseType IN :focusPhaseTypes
          AND i.endedAt > i.startedAt AND i.endedAt <= :now
        GROUP BY s.todoId
        """)
    List<TodoLastWorkedAtProjection> findLastCompletedWorkByTodos(
            @Param("userId") UUID userId,
            @Param("todoIds") Collection<UUID> todoIds,
            @Param("focusPhaseTypes") Collection<PomodoroPhaseType> focusPhaseTypes,
            @Param("now") LocalDateTime now
    );

    @Query("""
        SELECT DISTINCT s.todoId
        FROM FocusInterval i JOIN FocusSession s ON i.sessionId = s.id
        JOIN ActiveFocusSession a ON a.sessionId = s.id AND a.userId = s.userId
        WHERE s.userId = :userId AND s.todoId IN :todoIds AND s.status = :runningStatus
          AND i.phaseType IN :focusPhaseTypes AND i.endedAt IS NULL AND i.startedAt < :now
          AND NOT EXISTS (
              SELECT newer.id FROM FocusInterval newer
              WHERE newer.sessionId = i.sessionId AND newer.startedAt > i.startedAt
          )
        """)
    List<UUID> findCurrentlyWorkingTodoIds(
            @Param("userId") UUID userId,
            @Param("todoIds") Collection<UUID> todoIds,
            @Param("focusPhaseTypes") Collection<PomodoroPhaseType> focusPhaseTypes,
            @Param("runningStatus") TimerStatus runningStatus,
            @Param("now") LocalDateTime now
    );
    Optional<FocusInterval> findTopBySessionIdOrderByStartedAtDesc(UUID sessionId);

    List<FocusInterval> findAllBySessionId(UUID sessionId);

    List<FocusInterval> findAllBySessionIdAndPhaseTypeAndRound(UUID sessionId, PomodoroPhaseType phaseType, Integer round);

    List<FocusInterval> findAllBySessionIdInOrderBySessionIdAscStartedAtDesc(Collection<UUID> sessionIds);

    @Query("""
    SELECT i
    FROM FocusInterval i
    JOIN FocusSession s ON i.sessionId = s.id
    WHERE s.userId = :userId
      AND i.endedAt IS NOT NULL
      AND i.phaseType IN :focusPhaseTypes
    ORDER BY i.startedAt ASC
    """)
    List<FocusInterval> findCompletedFocusIntervalsByUser(
            @Param("userId") UUID userId,
            @Param("focusPhaseTypes") Collection<PomodoroPhaseType> focusPhaseTypes
    );

    @Query(value = """
    SELECT 
        DATE(i.started_at) AS date,
        SUM(TIMESTAMPDIFF(SECOND, i.started_at, i.ended_at)) AS total_seconds
    FROM focus_interval i
    JOIN focus_session s ON i.session_id = s.id
    WHERE s.user_id = :userId
      AND i.phase_type IN ('FOCUS', 'NORMAL')
    GROUP BY DATE(i.started_at)
    ORDER BY DATE(i.started_at)
    """, nativeQuery = true)
    List<Object[]> findDailyFocusDurationsByUser(@Param("userId") UUID userId);

    @Query("""
    SELECT i
    FROM FocusInterval i
    JOIN FocusSession s ON i.sessionId = s.id
    WHERE s.userId = :userId
      AND i.startedAt BETWEEN :start AND :end
    """)
    List<FocusInterval> findByUserAndPeriod(
            @Param("userId") UUID userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

}
