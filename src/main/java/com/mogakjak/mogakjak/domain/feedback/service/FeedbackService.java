package com.mogakjak.mogakjak.domain.feedback.service;

import com.mogakjak.mogakjak.domain.feedback.dto.request.FeedbackCreateRequest;
import com.mogakjak.mogakjak.domain.feedback.dto.response.FeedbackResponse;
import com.mogakjak.mogakjak.domain.user.entity.User;

public interface FeedbackService {
    FeedbackResponse createFeedback(User user, FeedbackCreateRequest request);
}
