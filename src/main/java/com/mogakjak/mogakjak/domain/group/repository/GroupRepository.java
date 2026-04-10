package com.mogakjak.mogakjak.domain.group.repository;

import com.mogakjak.mogakjak.domain.group.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GroupRepository extends JpaRepository<Group, UUID> {

    Optional<Group> findFirstByIsOfficialLoungeTrue();

    boolean existsByIsOfficialLoungeTrue();
}
