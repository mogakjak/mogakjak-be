package com.mogakjak.mogakjak.domain.timer.repository;

import com.mogakjak.mogakjak.domain.timer.entity.FocusInterval;
import com.mogakjak.mogakjak.domain.timer.entity.GroupFocusInterval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupFocusIntervalRepository extends JpaRepository<GroupFocusInterval, UUID> {
    Optional<GroupFocusInterval> findTopBySessionIdOrderByStartedAtDesc(UUID sessionId);

    List<GroupFocusInterval> findAllBySessionId(UUID sessionId);
}
