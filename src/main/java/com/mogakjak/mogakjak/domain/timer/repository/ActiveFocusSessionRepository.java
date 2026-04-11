package com.mogakjak.mogakjak.domain.timer.repository;

import com.mogakjak.mogakjak.domain.timer.entity.ActiveFocusSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActiveFocusSessionRepository extends JpaRepository<ActiveFocusSession, UUID> {
    Optional<ActiveFocusSession> findByUserId(UUID id);

    List<ActiveFocusSession> findAllByUserIdIn(Collection<UUID> userIds);
}
