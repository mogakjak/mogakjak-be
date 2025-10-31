package com.mogakjak.mogakjak.domain.user.entity;

import com.mogakjak.mogakjak.global.common.BaseSchema;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Todo extends BaseSchema {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Size(max = 35)
    @Column(nullable = false, length = 35)
    private String task;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private Integer targetTimeInSeconds;

    @Column(nullable = false)
    @Builder.Default
    private Integer actualTimeInSeconds = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isCompleted = false;

    public void updateInfo(Category category, String task, LocalDate date, Integer targetTimeInSeconds) {
        this.category = category;
        this.task = task;
        this.date = date;
        this.targetTimeInSeconds = targetTimeInSeconds;
    }

    public void toggleComplete() {
        this.isCompleted = !this.isCompleted;
    }
}