package com.mogakjak.mogakjak.domain.user.repository;

import com.mogakjak.mogakjak.domain.user.entity.Category;
import com.mogakjak.mogakjak.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    // 유저의 모든 카테고리를 순서(displayOrder)대로 조회
    List<Category> findAllByUserOrderByDisplayOrderAsc(User user);

    // 유저가 소유한 모든 카테고리 조회
    List<Category> findAllByUser(User user);

    // 특정 카테고리 ID와 유저로 조회 (소유권 검증)
    Optional<Category> findByIdAndUser(UUID id, User user);

    // 유저의 카테고리 중 가장 큰 displayOrder 값을 조회
    @Query("SELECT MAX(c.displayOrder) FROM Category c WHERE c.user = :user")
    Optional<Integer> findMaxDisplayOrderByUser(@Param("user") User user);
}