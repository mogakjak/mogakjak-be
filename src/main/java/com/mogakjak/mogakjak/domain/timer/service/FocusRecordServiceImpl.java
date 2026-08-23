package com.mogakjak.mogakjak.domain.timer.service;

import com.mogakjak.mogakjak.domain.timer.dto.response.DailyFocusStatsResponse;
import com.mogakjak.mogakjak.domain.timer.dto.response.DashboardResponse;
import com.mogakjak.mogakjak.domain.timer.entity.FocusInterval;
import com.mogakjak.mogakjak.domain.timer.enumerate.DashboardRangeType;
import com.mogakjak.mogakjak.domain.timer.repository.FocusIntervalRepository;
import com.mogakjak.mogakjak.domain.timer.util.DashboardRange;
import com.mogakjak.mogakjak.domain.todo.repository.TodoRepository;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.repository.CategoryRepository;
import com.mogakjak.mogakjak.global.enumerate.CategoryColor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FocusRecordServiceImpl implements FocusRecordService {

    private final FocusTimeAggregationService focusTimeAggregationService;
    private final FocusIntervalRepository focusIntervalRepository;
    private final TodoRepository todoRepository;
    private final CategoryRepository categoryRepository;

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
            result.add(DailyFocusStatsResponse.from(
                    day,
                    map.getOrDefault(day, 0L),
                    day.getDayOfWeek().getValue()
            ));
        }

        return result;
    }

    @Override
    public DashboardResponse getDashboard(User user, DashboardRangeType rangeType) {

        UUID userId = user.getId();

        DashboardRange range = DashboardRange.of(rangeType);
        LocalDateTime start = range.start();
        LocalDateTime end = range.end();

        /* --- SUMMARY 계산 --- */
        FocusTimeMetrics metrics = focusTimeAggregationService.getMetrics(userId, start, end);

        LocalDate rangeStartDate = start.toLocalDate();
        Integer completedTodoCount = todoRepository.countCompletedByUserBetween(userId, rangeStartDate, end.toLocalDate());

        DashboardResponse.Summary summary = new DashboardResponse.Summary(
                metrics.totalSeconds(),
                metrics.groupSeconds(),
                metrics.personalSeconds(),
                completedTodoCount
        );

        /* --- 시간대별 FocusInterval --- */
        List<FocusInterval> intervals =
                focusIntervalRepository.findByUserAndPeriod(userId, start, end);

        Map<Integer, Long> hourlyMap = new HashMap<>();
        for (FocusInterval interval : intervals) {
            splitIntervalByHour(interval)
                    .forEach((h, sec) -> hourlyMap.merge(h, sec, Long::sum));
        }

        List<DashboardResponse.HourlyFocus> hourlyFocus =
                IntStream.range(0, 24)
                        .mapToObj(h -> new DashboardResponse.HourlyFocus(
                                h,
                                hourlyMap.getOrDefault(h, 0L)
                        )).toList();

        /* --- CATEGORY 분포 --- */
        List<DashboardResponse.CategoryFocus> categoryRows = getCategoryStats(userId, start, end);

        List<DashboardResponse.CategoryFocus> categoryFocus = categoryRows.stream()
                .map(row -> new DashboardResponse.CategoryFocus(
                        row.categoryId(),
                        row.categoryName(),
                        row.color(),
                        row.totalSeconds(),
                        row.completedTodoCount(),
                        row.totalTodoCount()
                ))
                .toList();


        /* --- 4. 연간 heatmap --- */
        List<DailyFocusStatsResponse> dailyFocus = getDailyFocusDurations(user);

        return DashboardResponse.from(
                summary,
                hourlyFocus,
                categoryFocus,
                dailyFocus
        );
    }

    /* ------------------- UTILS ------------------- */

    private Map<Integer, Long> splitIntervalByHour(FocusInterval interval) {
        Map<Integer, Long> map = new HashMap<>();
        LocalDateTime cur = interval.getStartedAt();
        LocalDateTime end = interval.getEndedAt();

        if (cur == null || end == null) { // 기록이 잘못된 인터벌 → 통계에서 제외
            return map;
        }

        while (cur.isBefore(end)) {
            int hour = cur.getHour();
            LocalDateTime hourEnd = cur.withMinute(0).withSecond(0).withNano(0).plusHours(1);
            LocalDateTime segmentEnd = hourEnd.isBefore(end) ? hourEnd : end;

            long sec = Duration.between(cur, segmentEnd).getSeconds();
            map.merge(hour, sec, Long::sum);

            cur = segmentEnd;
        }
        return map;
    }

    private List<DashboardResponse.CategoryFocus> getCategoryStats(UUID userId, LocalDateTime start, LocalDateTime end) {
        List<Map<String, Object>> raw = categoryRepository.getRawCategoryStats(userId, start, end);

        return raw.stream()
                .map(map -> new DashboardResponse.CategoryFocus(
                        bytesToUUID((byte[]) map.get("categoryId")),
                        (String) map.get("categoryName"),
                        CategoryColor.valueOf((String) map.get("color")),
                        ((Number) map.get("totalSeconds")).longValue(),
                        ((Number) map.get("completedTodoCount")).intValue(),
                        ((Number) map.get("totalTodoCount")).intValue()
                ))
                .toList();
    }

    private UUID bytesToUUID(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long high = bb.getLong();
        long low = bb.getLong();
        return new UUID(high, low);
    }

}
