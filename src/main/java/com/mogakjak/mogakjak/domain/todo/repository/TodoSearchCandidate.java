package com.mogakjak.mogakjak.domain.todo.repository;

import java.time.LocalDateTime;
import java.util.UUID;

public interface TodoSearchCandidate {
    UUID getId();
    String getTask();
    LocalDateTime getCreatedAt();
}
