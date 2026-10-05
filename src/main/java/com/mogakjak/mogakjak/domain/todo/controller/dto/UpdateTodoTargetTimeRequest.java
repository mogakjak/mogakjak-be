package com.mogakjak.mogakjak.domain.todo.controller.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Range;

@Getter
@NoArgsConstructor
public class UpdateTodoTargetTimeRequest {

    @Range(min = 60, max = 86400, message = "목표 시간은 1분(60초)에서 24시간(86400초) 사이여야 합니다.")
    @Schema(description = "할 일의 누적 목표시간(초). 필드는 필수이며 null은 목표시간 해제", example = "3600",
            nullable = true, requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer targetTimeInSeconds;

    @JsonIgnore
    private boolean targetTimeProvided;

    @JsonSetter("targetTimeInSeconds")
    public void setTargetTimeInSeconds(Integer targetTimeInSeconds) {
        this.targetTimeInSeconds = targetTimeInSeconds;
        this.targetTimeProvided = true;
    }

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "targetTimeInSeconds 필드를 전달해야 합니다. 해제하려면 null을 전달하세요.")
    public boolean isTargetTimeProvided() {
        return targetTimeProvided;
    }
}
