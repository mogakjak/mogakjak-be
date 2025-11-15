package com.mogakjak.mogakjak.domain.user.repository;

import com.mogakjak.mogakjak.domain.user.entity.ImageCharacter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ImageCharacterRepository extends JpaRepository<ImageCharacter, UUID> {

    // 채소 도감 조회 (레벨 순 정렬)
    List<ImageCharacter> findAllByOrderByLevelAsc();

    Optional<ImageCharacter> findFirstByLevelAndIsActiveTrueOrderByCreatedAtAsc(Integer level);
}