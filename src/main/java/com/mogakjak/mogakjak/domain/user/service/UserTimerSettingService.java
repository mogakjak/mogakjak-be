package com.mogakjak.mogakjak.domain.user.service;

import com.mogakjak.mogakjak.domain.user.controller.dto.TimerTabOrderRequest;
import com.mogakjak.mogakjak.domain.user.controller.dto.TimerTabOrderResponse;
import com.mogakjak.mogakjak.domain.user.entity.User;

public interface UserTimerSettingService {

    // 타이머 탭 순서 조회
    TimerTabOrderResponse getTimerTabOrder(User user);

    // 타이머 탭 순서 변경
    void updateTimerTabOrder(User user, TimerTabOrderRequest request);
}