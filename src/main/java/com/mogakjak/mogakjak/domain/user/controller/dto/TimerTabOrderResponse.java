package com.mogakjak.mogakjak.domain.user.controller.dto;

import com.mogakjak.mogakjak.domain.timer.enumerate.TimerMode;
import com.mogakjak.mogakjak.domain.user.entity.UserTimerSetting;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TimerTabOrderResponse {

    private List<TimerMode> tabOrder;

    public static TimerTabOrderResponse from(UserTimerSetting setting) {
        return TimerTabOrderResponse.builder()
                .tabOrder(setting.getTimerTabOrder())
                .build();
    }
}