package com.mogakjak.mogakjak.domain.feedback.entity;

import com.mogakjak.mogakjak.domain.feedback.enumerate.FeedbackTagType;
import com.mogakjak.mogakjak.global.common.BaseSchema;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedbackTag extends BaseSchema {

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String displayName;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FeedbackTagType type;

    @Column(nullable = false)
    private boolean active;

    public static FeedbackTag create(String code, String displayName, FeedbackTagType type) {
        return new FeedbackTag(code, displayName, type, true);
    }
}
