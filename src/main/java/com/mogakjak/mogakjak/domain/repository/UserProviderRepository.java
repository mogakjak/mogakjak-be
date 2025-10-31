package com.mogakjak.mogakjak.domain.repository;

import com.mogakjak.mogakjak.domain.entity.UserProvider;
import com.mogakjak.mogakjak.domain.enumerate.ProviderType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserProviderRepository extends JpaRepository<UserProvider, UUID> {
    Optional<UserProvider> findByProviderIdAndType(String providerId, ProviderType type);
}
