package com.mogakjak.mogakjak.domain.feedback.service;

import com.mogakjak.mogakjak.domain.feedback.dto.request.FeedbackTagCreateRequest;
import com.mogakjak.mogakjak.domain.feedback.dto.response.FeedbackTagResponse;
import com.mogakjak.mogakjak.domain.feedback.entity.FeedbackTag;
import com.mogakjak.mogakjak.domain.feedback.enumerate.FeedbackTagType;
import com.mogakjak.mogakjak.domain.feedback.repository.FeedbackTagRepository;
import com.mogakjak.mogakjak.global.exception.CustomException;
import com.mogakjak.mogakjak.global.exception.status.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackTagServiceImpl implements FeedbackTagService {

    private final FeedbackTagRepository feedbackTagRepository;

    @Override
    @Transactional
    public void registerTags(List<FeedbackTagCreateRequest> requests) {

        for (FeedbackTagCreateRequest req : requests) {
            if (feedbackTagRepository.existsByCode(req.code())) {
                throw new CustomException(ErrorCode.DUPLICATE_FEEDBACK_TAG);
            }

            FeedbackTag tag = FeedbackTag.create(
                    req.code(),
                    req.displayName(),
                    req.type()
            );
            feedbackTagRepository.save(tag);
        }
    }

    @Override
    public List<FeedbackTagResponse> getAllTags(FeedbackTagType type) {
        if (type == null) {
            return feedbackTagRepository.findAllByActiveTrue();
        }
        return feedbackTagRepository.findAllByTypeAndActiveTrue(type);
    }
}
