package com.mogakjak.mogakjak.domain.timer.repository;

import com.mogakjak.mogakjak.domain.timer.entity.FocusSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FocusSessionRepository extends JpaRepository<FocusSession, UUID> {
}
