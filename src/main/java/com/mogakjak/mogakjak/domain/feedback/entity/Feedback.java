package com.mogakjak.mogakjak.domain.feedback.entity;

import com.mogakjak.mogakjak.global.common.BaseSchema;
import jakarta.persistence.*;
import lombok.*;

import java.util.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Feedback extends BaseSchema {

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private int score;

    private String content;

    @OneToMany(mappedBy = "feedback", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FeedbackTagRelation> tagRelations = new ArrayList<>();

    public static Feedback create(UUID userId, int score, String content) {
        return new Feedback(
                userId,
                score,
                content,
                new ArrayList<>()
        );
    }

    public void addTag(FeedbackTag tag) {
        this.tagRelations.add(FeedbackTagRelation.of(this, tag));
    }
}
