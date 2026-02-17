package com.example.aegis_be.domain.eventlog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "AI 분석 텍스트 저장 요청")
@Getter
@NoArgsConstructor
public class EventLogSaveRequest {

    @Schema(
            description = "AI가 분석한 텍스트 (줄글)",
            example = "환자가 흉통을 호소하며 좌측 팔 저림 증상 동반. 심근경색 가능성 높음.",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "텍스트는 필수입니다")
    private String description;
}
