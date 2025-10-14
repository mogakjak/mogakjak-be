package com.mogakjak.mogakjak.global.auth.repository;

import com.mogakjak.mogakjak.global.auth.schema.RefreshToken;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefreshTokenRedisRepository extends CrudRepository<RefreshToken, String> {

    List<RefreshToken> findByUserId(String userId);
}
