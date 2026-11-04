package com.mogakjak.mogakjak.domain.user.controller;

import com.mogakjak.mogakjak.domain.timer.service.FocusAttendanceService;
import com.mogakjak.mogakjak.domain.timer.service.FocusAttendanceSummary;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.service.MyPageAttendanceService;
import com.mogakjak.mogakjak.domain.user.service.MyPageService;
import com.mogakjak.mogakjak.global.auth.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MyPageAttendanceControllerTest {
    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsTenDaysForAuthenticatedUserAndIgnoresForeignUserParameter() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", userId);
        CustomUserDetails principal = CustomUserDetails.of(user);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, "", principal.getAuthorities()));
        FocusAttendanceService attendance = mock(FocusAttendanceService.class);
        LocalDate today = LocalDate.of(2026, 10, 19);
        when(attendance.getSummary(userId, today.atTime(0, 5)))
                .thenReturn(new FocusAttendanceSummary(3, Map.of(today, 1_800L)));
        var calendar = new MyPageAttendanceService(attendance,
                Clock.fixed(Instant.parse("2026-10-18T15:05:00Z"), ZoneOffset.UTC));
        var mvc = MockMvcBuilders.standaloneSetup(new MyPageController(mock(MyPageService.class), calendar))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();

        mvc.perform(get("/api/mypage/attendance").param("userId", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.referenceDate").value("2026-10-19"))
                .andExpect(jsonPath("$.data.timeZone").value("Asia/Seoul"))
                .andExpect(jsonPath("$.data.attendanceThresholdInSeconds").value(1_800))
                .andExpect(jsonPath("$.data.totalAttendanceDays").value(3))
                .andExpect(jsonPath("$.data.days.length()").value(10))
                .andExpect(jsonPath("$.data.days[0].date").value("2026-10-14"))
                .andExpect(jsonPath("$.data.days[5].date").value("2026-10-19"))
                .andExpect(jsonPath("$.data.days[5].focusTimeInSeconds").value(1_800))
                .andExpect(jsonPath("$.data.days[5].attended").value(true))
                .andExpect(jsonPath("$.data.days[5].today").value(true))
                .andExpect(jsonPath("$.data.days[6].future").value(true))
                .andExpect(jsonPath("$.data.days[9].date").value("2026-10-23"));
        verify(attendance).getSummary(userId, LocalDateTime.of(2026, 10, 19, 0, 5));
    }
}
