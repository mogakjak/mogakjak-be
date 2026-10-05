package com.mogakjak.mogakjak.domain.todo.controller.dto;

import com.mogakjak.mogakjak.domain.timer.entity.FocusSession;
import com.mogakjak.mogakjak.domain.timer.enumerate.ParticipationType;
import com.mogakjak.mogakjak.domain.timer.enumerate.PomodoroPhaseType;
import com.mogakjak.mogakjak.domain.timer.enumerate.TimerMode;
import com.mogakjak.mogakjak.domain.timer.enumerate.TimerStatus;
import com.mogakjak.mogakjak.domain.timer.service.FocusSessionSnapshotCalculator.Snapshot;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

public record ActiveTodoSessionResponse(
        UUID sessionId,
        TimerMode mode,
        TimerStatus status,
        ParticipationType participationType,
        UUID groupId,
        @JsonFormat(shape = JsonFormat.Shape.STRING) OffsetDateTime startedAt,
        @Schema(description = "카운트다운 설정(초). 할 일 목표시간과 별개", nullable = true)
        Long targetDuration,
        @Schema(description = "뽀모도로 집중 구간 설정(초)", nullable = true)
        Long focusDuration,
        @Schema(description = "뽀모도로 휴식 구간 설정(초)", nullable = true)
        Long breakDuration,
        Integer repeatCount,
        @Schema(description = "현재 구간 NORMAL/FOCUS/BREAK. 기록이 없으면 null", nullable = true)
        PomodoroPhaseType phaseType,
        Integer round,
        @JsonFormat(shape = JsonFormat.Shape.STRING) OffsetDateTime currentIntervalStartedAt,
        @Schema(description = "이번 세션의 집중 구간 합계(초). 이전 세션과 휴식 제외", example = "360")
        long focusTimeInSeconds,
        @Schema(description = "현재 단계·라운드의 경과 시간(초). 일시정지 후 재개 구간을 합산", example = "60")
        long phaseElapsedTimeInSeconds,
        @Schema(description = "최신 열린 구간이 RUNNING 상태로 실제 진행 중인지 여부")
        boolean isCounting
) {
    public static ActiveTodoSessionResponse from(FocusSession session, Snapshot snapshot) {
        var current = snapshot.currentInterval();
        return new ActiveTodoSessionResponse(session.getId(), session.getMode(), session.getStatus(),
                session.getParticipationType(), session.getGroupId(), toSeoul(session.getStartedAt()),
                session.getTargetDuration(), session.getFocusDuration(), session.getBreakDuration(),
                session.getRepeatCount(), current == null ? null : current.getPhaseType(),
                current == null ? null : current.getRound(), current == null ? null : toSeoul(current.getStartedAt()),
                snapshot.focusSeconds(), snapshot.phaseSeconds(), snapshot.isCounting());
    }

    private static OffsetDateTime toSeoul(LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("Asia/Seoul")).toOffsetDateTime();
    }
}
