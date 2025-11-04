package com.mogakjak.mogakjak.domain.timer.repository;

import com.mogakjak.mogakjak.domain.timer.entity.TimerSession;
import com.mogakjak.mogakjak.domain.timer.enumerate.TimerStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TimerSessionRepository extends JpaRepository<TimerSession, UUID> {
    Optional<TimerSession> findByUserIdAndStatus(UUID userId, TimerStatus status);
}
