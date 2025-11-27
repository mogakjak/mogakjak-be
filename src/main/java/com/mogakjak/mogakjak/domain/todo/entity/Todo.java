package com.mogakjak.mogakjak.domain.todo.entity;

import com.mogakjak.mogakjak.global.common.BaseSchema;
import com.mogakjak.mogakjak.domain.user.entity.Category;
import com.mogakjak.mogakjak.domain.user.entity.User;
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

    @Column(nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    public void updateInfo(Category category, String task, LocalDate date, Integer targetTimeInSeconds) {
        this.category = category;
        this.task = task;
        this.date = date;
        this.targetTimeInSeconds = targetTimeInSeconds;
    }

    public void toggleComplete() {
        this.isCompleted = !this.isCompleted;
    }

    public void softDelete() { this.isDeleted = true; }

    public void addActualTime(Long seconds) {
        if (seconds != null && seconds > 0) {
            this.actualTimeInSeconds = (this.actualTimeInSeconds != null ? this.actualTimeInSeconds : 0) + seconds.intValue();
        }
    }

    public void updateActualTime(Integer actualTimeInSeconds) {
        this.actualTimeInSeconds = actualTimeInSeconds != null ? actualTimeInSeconds : 0;
    }
}