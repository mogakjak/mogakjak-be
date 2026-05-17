package com.mogakjak.mogakjak.domain.user.controller.dto;

import com.mogakjak.mogakjak.domain.timer.enumerate.TimerMode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class TimerTabOrderRequest {

    @NotNull(message = "타이머 탭 순서는 필수입니다.")
    @Size(min = 3, max = 3, message = "타이머 탭은 3개여야 합니다.")
    private List<TimerMode> tabOrder;
}