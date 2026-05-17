package com.mogakjak.mogakjak.domain.user.entity;

import com.mogakjak.mogakjak.domain.timer.enumerate.TimerMode;
import com.mogakjak.mogakjak.global.common.BaseSchema;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTimerSetting extends BaseSchema {

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @ElementCollection
    @CollectionTable(
            name = "user_timer_tab_order",
            joinColumns = @JoinColumn(name = "user_timer_setting_id")
    )
    @OrderColumn(name = "position")
    @Column(name = "timer_mode")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private List<TimerMode> timerTabOrder = List.of(TimerMode.POMODORO, TimerMode.TIMER, TimerMode.STOPWATCH);

    public static UserTimerSetting createDefault(User user) {
        return UserTimerSetting.builder()
                .user(user)
                .build();
    }

    public void updateTimerTabOrder(List<TimerMode> timerTabOrder) {
        this.timerTabOrder = timerTabOrder;
    }
}