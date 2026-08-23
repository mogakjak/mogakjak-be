package com.mogakjak.mogakjak.domain.timer.service;

import com.mogakjak.mogakjak.domain.timer.repository.FocusSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FocusTimeAggregationServiceTest {

    @Mock
    private FocusSessionRepository focusSessionRepository;

    @InjectMocks
    private FocusTimeAggregationService service;

    @Test
    void getMetrics_usesFocusSessionsForTotalGroupAndPersonalTime() {
        UUID userId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.of(2026, 8, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 9, 1, 0, 0);
        when(focusSessionRepository.sumTotalSeconds(userId, start, end)).thenReturn(10_800L);
        when(focusSessionRepository.sumGroupSeconds(userId, start, end)).thenReturn(7_200L);
        when(focusSessionRepository.sumPersonalSeconds(userId, start, end)).thenReturn(3_600L);

        FocusTimeMetrics metrics = service.getMetrics(userId, start, end);

        assertEquals(10_800L, metrics.totalSeconds());
        assertEquals(7_200L, metrics.groupSeconds());
        assertEquals(3_600L, metrics.personalSeconds());
    }

    @Test
    void getLifetimeSeconds_usesAllPersonalFocusSessions() {
        UUID userId = UUID.randomUUID();
        when(focusSessionRepository.sumLifetimeSeconds(userId)).thenReturn(12_345L);

        assertEquals(12_345L, service.getLifetimeSeconds(userId));
    }
}
