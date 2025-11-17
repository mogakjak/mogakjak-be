package com.mogakjak.mogakjak.domain.feedback.repository;

import com.mogakjak.mogakjak.domain.feedback.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {
}
