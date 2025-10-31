package com.mogakjak.mogakjak.domain.repository;

import com.mogakjak.mogakjak.domain.entity.User;
import com.mogakjak.mogakjak.domain.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    // User로 UserProfile을 조회 (대표 캐릭터/이미지 조회 시 N+1 방지)
    @Query("SELECT up FROM UserProfile up JOIN FETCH up.mainImageCharacter WHERE up.user = :user")
    Optional<UserProfile> findByUserWithMainCharacter(User user);

    Optional<UserProfile> findByUser(User user);
}