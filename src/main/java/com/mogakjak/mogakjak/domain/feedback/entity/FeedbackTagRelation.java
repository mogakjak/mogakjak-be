package com.mogakjak.mogakjak.domain.feedback.entity;

import com.mogakjak.mogakjak.global.common.BaseSchema;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedbackTagRelation extends BaseSchema {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feedback_id", nullable = false)
    private Feedback feedback;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private FeedbackTag tag;

    public static FeedbackTagRelation of(Feedback feedback, FeedbackTag tag) {
        return new FeedbackTagRelation(feedback, tag);
    }
}
