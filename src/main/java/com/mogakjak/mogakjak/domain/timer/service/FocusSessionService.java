package com.mogakjak.mogakjak.domain.timer.service;

import com.mogakjak.mogakjak.domain.timer.dto.request.TimerStartRequest;
import com.mogakjak.mogakjak.domain.timer.dto.response.TimerResponse;
import com.mogakjak.mogakjak.domain.user.entity.User;

import java.util.UUID;

public interface FocusSessionService {

    /**
     * 타이머 시작
     */
    TimerResponse startTimer(User user, TimerStartRequest request);

    /**
     * 타이머 일시정지
     */
    TimerResponse pauseTimer(User user, UUID sessionId);

    /**
     * 타이머 재개
     */
    TimerResponse resumeTimer(User user, UUID sessionId);

    /**
     * 타이머 종료
     */
    TimerResponse finishTimer(User user, UUID sessionId);
//
//    /**
//     * (뽀모도로의 경우) 다음 단계로 전환
//     */
//    void nextPomodoroPhase(User user, UUID sessionId);
//
//    /**
//     * 일일 몰입량 통계
//     */
//    List<DailyFocusStatsResponse> getDailyFocusDurations(User user);
}
