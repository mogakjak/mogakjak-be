package com.mogakjak.mogakjak.domain.lounge.repository;

import com.mogakjak.mogakjak.domain.lounge.entity.OfficialLoungeAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OfficialLoungeAccessLogRepository extends JpaRepository<OfficialLoungeAccessLog, UUID> {
}
