package com.mogakjak.mogakjak.domain.feedback.repository;

import com.mogakjak.mogakjak.domain.feedback.entity.FeedbackTagRelation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FeedbackTagRelationRepository extends JpaRepository<FeedbackTagRelation, UUID> {
}
