package com.mogakjak.mogakjak.domain.timer.service;

import com.mogakjak.mogakjak.domain.timer.repository.FocusSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FocusTimeAggregationService {

    private final FocusSessionRepository focusSessionRepository;

    public FocusTimeMetrics getMetrics(UUID userId, LocalDateTime start, LocalDateTime end) {
        long personalSeconds = focusSessionRepository.sumPersonalSeconds(userId, start, end);
        long groupSeconds = focusSessionRepository.sumGroupSeconds(userId, start, end);
        long totalSeconds = focusSessionRepository.sumTotalSeconds(userId, start, end);
        return new FocusTimeMetrics(totalSeconds, groupSeconds, personalSeconds);
    }

    public long getLifetimeSeconds(UUID userId) {
        return focusSessionRepository.sumLifetimeSeconds(userId);
    }
}
