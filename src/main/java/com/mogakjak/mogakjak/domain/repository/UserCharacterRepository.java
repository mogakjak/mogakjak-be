package com.mogakjak.mogakjak.domain.repository;

import com.mogakjak.mogakjak.domain.entity.ImageCharacter;
import com.mogakjak.mogakjak.domain.entity.UserCharacter;
import com.mogakjak.mogakjak.domain.entity.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserCharacterRepository extends JpaRepository<UserCharacter, UUID> {

    @Query("SELECT uc FROM UserCharacter uc JOIN FETCH uc.imageCharacter WHERE uc.user = :user")
    List<UserCharacter> findAllByUserWithImageCharacter(@Param("user") User user);

    boolean existsByUserAndImageCharacter(User user, ImageCharacter imageCharacter);
}