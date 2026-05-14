package com.mogakjak.mogakjak.domain.timer.repository;

import com.mogakjak.mogakjak.domain.timer.entity.FocusInterval;
import com.mogakjak.mogakjak.domain.timer.enumerate.PomodoroPhaseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FocusIntervalRepository extends JpaRepository<FocusInterval, UUID> {
    Optional<FocusInterval> findTopBySessionIdOrderByStartedAtDesc(UUID sessionId);

    List<FocusInterval> findAllBySessionId(UUID sessionId);

    List<FocusInterval> findAllBySessionIdAndPhaseTypeAndRound(UUID sessionId, PomodoroPhaseType phaseType, Integer round);

    List<FocusInterval> findAllBySessionIdInOrderBySessionIdAscStartedAtDesc(Collection<UUID> sessionIds);

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
