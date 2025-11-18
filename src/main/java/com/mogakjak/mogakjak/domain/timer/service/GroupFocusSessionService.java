package com.mogakjak.mogakjak.domain.timer.service;

import com.mogakjak.mogakjak.domain.timer.dto.request.GroupTimerStartRequest;
import com.mogakjak.mogakjak.domain.timer.dto.response.TimerResponse;
import com.mogakjak.mogakjak.domain.user.entity.User;

import java.util.UUID;

public interface GroupFocusSessionService {
    TimerResponse startGroupTimer(User user, UUID groupId, GroupTimerStartRequest request);

    TimerResponse pauseSession(User user, UUID groupId, UUID sessionId);

    TimerResponse resumeSession(User user, UUID groupId, UUID sessionId);

    TimerResponse finishSession(User user, UUID groupId, UUID sessionId);
}
