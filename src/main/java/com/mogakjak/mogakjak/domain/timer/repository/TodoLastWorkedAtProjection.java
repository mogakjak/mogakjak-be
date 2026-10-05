package com.mogakjak.mogakjak.domain.timer.repository;

import java.time.LocalDateTime;
import java.util.UUID;

public interface TodoLastWorkedAtProjection {
    UUID getTodoId();
    LocalDateTime getLastWorkedAt();
}
