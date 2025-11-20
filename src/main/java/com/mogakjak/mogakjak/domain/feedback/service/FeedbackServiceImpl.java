package com.mogakjak.mogakjak.domain.feedback.service;

import com.mogakjak.mogakjak.domain.feedback.dto.request.FeedbackCreateRequest;
import com.mogakjak.mogakjak.domain.feedback.dto.response.FeedbackResponse;
import com.mogakjak.mogakjak.domain.feedback.entity.Feedback;
import com.mogakjak.mogakjak.domain.feedback.entity.FeedbackTag;
import com.mogakjak.mogakjak.domain.feedback.enumerate.FeedbackTagType;
import com.mogakjak.mogakjak.domain.feedback.repository.FeedbackRepository;
import com.mogakjak.mogakjak.domain.feedback.repository.FeedbackTagRepository;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.global.exception.CustomException;
import com.mogakjak.mogakjak.global.exception.status.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final FeedbackTagRepository tagRepository;

    @Override
    @Transactional
    public FeedbackResponse createFeedback(User user, FeedbackCreateRequest request) {
        int score = request.score();
        if (score < 1 || score > 5) {
            throw new CustomException(ErrorCode.INVALID_FEEDBACK_SCORE);
        }

        FeedbackTagType allowedType = getAllowedType(score);

        Feedback feedback = Feedback.create(
                user.getId(),
                score,
                request.content()
        );

        if (request.tagCodes() != null && !request.tagCodes().isEmpty()) {
            List<FeedbackTag> tags = tagRepository.findAllByCodeIn(request.tagCodes());
            if (tags.size() != request.tagCodes().size()) {
                throw new CustomException(ErrorCode.FEEDBACK_TAG_NOT_FOUND);
            }
            for (FeedbackTag tag : tags) {
                if (tag.getType() != allowedType) {
                    throw new CustomException(ErrorCode.INVALID_FEEDBACK_TAG_TYPE);
                }
                feedback.addTag(tag);
            }
        }

        feedbackRepository.save(feedback);
        return FeedbackResponse.from(feedback);
    }

    private FeedbackTagType getAllowedType(int score) {
        if (score <= 2) return FeedbackTagType.NEGATIVE;
        if (score == 3) return FeedbackTagType.NEUTRAL;
        return FeedbackTagType.POSITIVE; // 4~5
    }

}
