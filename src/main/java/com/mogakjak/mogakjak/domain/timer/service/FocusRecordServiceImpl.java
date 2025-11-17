package com.mogakjak.mogakjak.domain.timer.service;

import com.mogakjak.mogakjak.domain.timer.dto.response.DailyFocusStatsResponse;
import com.mogakjak.mogakjak.domain.timer.repository.ActiveFocusSessionRepository;
import com.mogakjak.mogakjak.domain.timer.repository.FocusIntervalRepository;
import com.mogakjak.mogakjak.domain.timer.repository.FocusSessionRepository;
import com.mogakjak.mogakjak.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FocusRecordServiceImpl implements FocusRecordService {

    private final FocusSessionRepository focusSessionRepository;
    private final FocusIntervalRepository focusIntervalRepository;
    private final ActiveFocusSessionRepository activeFocusSessionRepository;

    @Override
    public List<DailyFocusStatsResponse> getDailyFocusDurations(User user) {
        Map<LocalDate, Long> map = focusIntervalRepository.findDailyFocusDurationsByUser(user.getId())
                .stream()
                .collect(Collectors.toMap(
                        row -> ((java.sql.Date) row[0]).toLocalDate(),
                        row -> ((Number) row[1]).longValue()
                ));

        List<DailyFocusStatsResponse> result = new ArrayList<>();

        Year now = Year.now();
        LocalDate start = now.atMonth(1).atDay(1);
        LocalDate end = now.atMonth(12).atEndOfMonth();

        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            result.add(new DailyFocusStatsResponse(
                    day,
                    map.getOrDefault(day, 0L)
            ));
        }

        return result;
    }
}
