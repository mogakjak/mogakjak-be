package com.mogakjak.mogakjak.domain.timer.service;

import com.mogakjak.mogakjak.domain.timer.dto.response.DailyFocusStatsResponse;
import com.mogakjak.mogakjak.domain.user.entity.User;

import java.util.List;

public interface FocusRecordService {
    List<DailyFocusStatsResponse> getDailyFocusDurations(User user);
}
