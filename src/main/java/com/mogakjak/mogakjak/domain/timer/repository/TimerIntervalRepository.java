package com.mogakjak.mogakjak.domain.timer.repository;

import com.mogakjak.mogakjak.domain.timer.entity.TimerInterval;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface TimerIntervalRepository extends JpaRepository<TimerInterval, UUID> {
    List<TimerInterval> findAllBySessionId(UUID sessionId);

    @Query(value = """
        SELECT DATE(i.started_at) AS date,
               SUM(TIMESTAMPDIFF(SECOND, i.started_at, i.ended_at)) AS total_seconds
        FROM timer_interval i
        JOIN timer_session s ON i.session_id = s.id
        WHERE s.user_id = :userId
          AND i.type IN ('FOCUS', 'NORMAL')
        GROUP BY DATE(i.started_at)
        ORDER BY DATE(i.started_at)
    """, nativeQuery = true)
    List<Object[]> findDailyFocusDurationsByUser(@Param("userId") UUID userId);

}
