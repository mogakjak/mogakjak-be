package com.mogakjak.mogakjak.domain.timer.service;

import com.mogakjak.mogakjak.domain.timer.dto.response.DashboardResponse;
import com.mogakjak.mogakjak.domain.timer.enumerate.DashboardRangeType;
import com.mogakjak.mogakjak.domain.timer.repository.FocusIntervalRepository;
import com.mogakjak.mogakjak.domain.todo.repository.TodoRepository;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FocusRecordServiceImplTest {

    @Mock private FocusTimeAggregationService focusTimeAggregationService;
    @Mock private FocusIntervalRepository focusIntervalRepository;
    @Mock private TodoRepository todoRepository;
    @Mock private CategoryRepository categoryRepository;

    @InjectMocks
    private FocusRecordServiceImpl service;

    @Test
    void getDashboard_usesUnifiedFocusTimeMetrics() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().name("user").email("user@example.com").build();
        ReflectionTestUtils.setField(user, "id", userId);
        when(focusTimeAggregationService.getMetrics(eq(userId), any(), any()))
                .thenReturn(new FocusTimeMetrics(10_800L, 7_200L, 3_600L));
        when(todoRepository.countCompletedByUserBetween(eq(userId), any(), any())).thenReturn(0);
        when(focusIntervalRepository.findByUserAndPeriod(eq(userId), any(), any())).thenReturn(List.of());
        when(categoryRepository.getRawCategoryStats(eq(userId), any(), any())).thenReturn(List.of());
        when(focusIntervalRepository.findDailyFocusDurationsByUser(userId)).thenReturn(List.of());

        DashboardResponse response = service.getDashboard(user, DashboardRangeType.MONTH);

        assertEquals(10_800L, response.summary().totalSeconds());
        assertEquals(7_200L, response.summary().groupSeconds());
        assertEquals(3_600L, response.summary().personalSeconds());
    }
}
