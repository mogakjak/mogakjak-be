package com.mogakjak.mogakjak.domain.user.repository;

import com.mogakjak.mogakjak.domain.user.entity.Todo;
import com.mogakjak.mogakjak.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TodoRepository extends JpaRepository<Todo, UUID> {

    // 유저와 특정 날짜로 모든 To-do 조회 (생성순)
    List<Todo> findAllByUserAndDateOrderByCreatedAtAsc(User user, LocalDate date);

    // 특정 To-do ID와 유저로 조회 (소유권 검증)
    Optional<Todo> findByIdAndUser(UUID id, User user);
}