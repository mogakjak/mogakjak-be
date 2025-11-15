package com.mogakjak.mogakjak.domain.quote.repository;

import com.mogakjak.mogakjak.domain.quote.entity.Quote;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface QuoteRepository extends JpaRepository<Quote, UUID> {

    @Query(value = "SELECT * FROM quote LIMIT 1 OFFSET :offset", nativeQuery = true)
    Quote findRandomByOffset(@Param("offset") int offset);
}
