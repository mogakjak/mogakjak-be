package com.mogakjak.mogakjak.domain.user.repository;

import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.entity.UserTimerSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserTimerSettingRepository extends JpaRepository<UserTimerSetting, UUID> {
    Optional<UserTimerSetting> findByUser(User user);
}