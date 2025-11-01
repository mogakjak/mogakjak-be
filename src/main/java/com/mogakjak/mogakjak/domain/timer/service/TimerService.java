package com.mogakjak.mogakjak.domain.timer.service;

import com.mogakjak.mogakjak.domain.timer.dto.request.TimerStartRequest;
import com.mogakjak.mogakjak.domain.timer.entity.TimerSession;
import com.mogakjak.mogakjak.domain.user.entity.User;

public interface TimerService {

    /**
     * 타이머 시작
     */
    TimerSession startTimer(User user, TimerStartRequest request);

    /**
     * 타이머 일시정지
     */
    void pauseTimer(User user);

    /**
     * 타이머 재개
     */
    void resumeTimer(User user);

    /**
     * 타이머 종료
     */
    TimerSession stopTimer(User user);
}
