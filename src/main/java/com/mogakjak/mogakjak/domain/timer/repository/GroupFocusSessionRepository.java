package com.mogakjak.mogakjak.domain.timer.repository;

import com.mogakjak.mogakjak.domain.timer.entity.GroupFocusSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GroupFocusSessionRepository extends JpaRepository<GroupFocusSession, UUID> {
}
