package com.mogakjak.mogakjak.domain.feedback.service;

import com.mogakjak.mogakjak.domain.feedback.dto.request.FeedbackTagCreateRequest;
import com.mogakjak.mogakjak.domain.feedback.dto.response.FeedbackTagResponse;
import com.mogakjak.mogakjak.domain.feedback.enumerate.FeedbackTagType;

import java.util.List;

public interface FeedbackTagService {

    void registerTags(List<FeedbackTagCreateRequest> requests);

    List<FeedbackTagResponse> getAllTags(FeedbackTagType type);
}
