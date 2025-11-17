package com.mogakjak.mogakjak.domain.feedback.repository;

import com.mogakjak.mogakjak.domain.feedback.dto.response.FeedbackTagResponse;
import com.mogakjak.mogakjak.domain.feedback.entity.FeedbackTag;
import com.mogakjak.mogakjak.domain.feedback.enumerate.FeedbackTagType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface FeedbackTagRepository extends JpaRepository<FeedbackTag, UUID> {
    boolean existsByCode(String code);
    Optional<FeedbackTag> findByCode(String code);
    List<FeedbackTag> findAllByType(FeedbackTagType type);
    List<FeedbackTagResponse> findAllByActiveTrue();
    List<FeedbackTagResponse> findAllByTypeAndActiveTrue(FeedbackTagType type);
}
