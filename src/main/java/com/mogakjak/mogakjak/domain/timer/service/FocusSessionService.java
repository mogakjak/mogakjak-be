package com.mogakjak.mogakjak.domain.timer.service;

import com.mogakjak.mogakjak.domain.timer.dto.request.StopwatchStartRequest;
import com.mogakjak.mogakjak.domain.timer.dto.request.TimerStartRequest;
import com.mogakjak.mogakjak.domain.timer.dto.response.TimerResponse;
import com.mogakjak.mogakjak.domain.user.entity.User;

import java.util.UUID;

public interface FocusSessionService {

    TimerResponse startTimer(User user, TimerStartRequest request);

    TimerResponse pauseSession(User user, UUID sessionId);

    TimerResponse resumeSession(User user, UUID sessionId);

    TimerResponse finishSession(User user, UUID sessionId);

    TimerResponse startStopwatch(User user, StopwatchStartRequest request);
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
