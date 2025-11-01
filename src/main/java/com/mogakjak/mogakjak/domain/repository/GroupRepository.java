package com.mogakjak.mogakjak.domain.repository;

import com.mogakjak.mogakjak.domain.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface GroupRepository extends JpaRepository<Group, UUID> {
}