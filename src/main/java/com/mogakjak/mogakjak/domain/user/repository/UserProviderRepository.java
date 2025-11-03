package com.mogakjak.mogakjak.domain.user.repository;

import com.mogakjak.mogakjak.global.enumerate.ProviderType;
import com.mogakjak.mogakjak.domain.user.entity.UserProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserProviderRepository extends JpaRepository<UserProvider, UUID> {
    Optional<UserProvider> findByProviderIdAndType(String providerId, ProviderType type);
}
