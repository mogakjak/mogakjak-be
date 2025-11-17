package com.mogakjak.mogakjak.domain.timer.dto.response;

import com.mogakjak.mogakjak.global.enumerate.CategoryColor;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;
import java.util.UUID;

@Schema(description = "대시보드 전체 응답")
public record DashboardResponse(

        @Schema(description = "요약 정보")
        Summary summary,

        @Schema(description = "시간대별 몰입 시간 통계")
        List<HourlyFocus> hourlyFocus,

        @Schema(description = "카테고리별 몰입 시간 통계")
        List<CategoryFocus> categoryFocus,

        @Schema(description = "연간 일별 몰입 시간 통계")
        List<DailyFocusStatsResponse> dailyFocus

) {

    @Schema(description = "요약 통계")
    public record Summary(
            @Schema(description = "총 몰입 시간")
            Long totalSeconds,

            @Schema(description = "모각작 몰입 시간")
            Long groupSeconds,

            @Schema(description = "개인 몰입 시간")
            Long personalSeconds,

            @Schema(description = "완료한 할 일 개수")
            Integer completedTodoCount
    ) {}

    @Schema(description = "카테고리별 통계")
    public record CategoryFocus(
            @Schema(description = "카테고리 Id")
            UUID categoryId,

            @Schema(description = "카테고리 이름")
            String categoryName,

            @Schema(description = "카테고리 색상")
            CategoryColor color,

            @Schema(description = "카테고리 별 총 몰입 시간")
            Long totalSeconds,

            @Schema(description = "카테고리 별 목표 달성 개수")
            Integer completedTodoCount,

            @Schema(description = "카테고리 별 총 목표 todo 개수")
            Integer totalTodoCount
    ) {}

    @Schema(description = "시간대별 통계")
    public record HourlyFocus(

            @Min(0) @Max(23)
            @Schema(description = "시간대 (0 ~ 23시)", example = "17")
            Integer hour,

            @Schema(description = "시간 당 집중한 시간(초 단위)", example = "600")
            Long totalSeconds
    ) {}

    public static DashboardResponse from(
            Summary summary,
            List<HourlyFocus> hourlyFocus,
            List<CategoryFocus> categoryFocus,
            List<DailyFocusStatsResponse> dailyFocus
    ) {
        return new DashboardResponse(summary, hourlyFocus, categoryFocus, dailyFocus);
    }

}
