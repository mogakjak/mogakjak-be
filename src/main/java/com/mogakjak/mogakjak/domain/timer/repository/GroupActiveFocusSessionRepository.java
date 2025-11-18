package com.mogakjak.mogakjak.domain.timer.repository;

import com.mogakjak.mogakjak.domain.timer.entity.GroupActiveFocusSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GroupActiveFocusSessionRepository extends JpaRepository<GroupActiveFocusSession, UUID> {
    Optional<GroupActiveFocusSession> findByGroupId(UUID groupId);
}
