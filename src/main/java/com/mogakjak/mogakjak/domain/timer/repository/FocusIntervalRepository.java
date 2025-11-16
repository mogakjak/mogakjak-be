package com.mogakjak.mogakjak.domain.timer.repository;

import com.mogakjak.mogakjak.domain.timer.entity.FocusInterval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FocusIntervalRepository extends JpaRepository<FocusInterval, UUID> {
}
