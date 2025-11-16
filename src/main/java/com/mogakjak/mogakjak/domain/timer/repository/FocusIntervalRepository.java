package com.mogakjak.mogakjak.domain.timer.repository;

import com.mogakjak.mogakjak.domain.timer.entity.FocusInterval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FocusIntervalRepository extends JpaRepository<FocusInterval, UUID> {
    Optional<FocusInterval> findTopBySessionIdOrderByStartedAtDesc(UUID sessionId);

    List<FocusInterval> findAllBySessionId(UUID sessionId);
}
