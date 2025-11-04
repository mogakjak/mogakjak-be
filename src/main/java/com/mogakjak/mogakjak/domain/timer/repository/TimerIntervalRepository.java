package com.mogakjak.mogakjak.domain.timer.repository;

import com.mogakjak.mogakjak.domain.timer.entity.TimerInterval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TimerIntervalRepository extends JpaRepository<TimerInterval, UUID> {
    List<TimerInterval> findAllBySessionId(UUID sessionId);
}
