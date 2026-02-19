package com.example.aegis_be.domain.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Schema(description = "주호소 + 평가소견 + 발생일시 부분 업데이트 요청 (구급대원용)")
@Getter
@NoArgsConstructor
public class AssessmentUpdateRequest {

    @Schema(description = "주호소", example = "가슴 통증, 호흡 곤란", nullable = true)
    private String chiefComplaint;

    @Schema(description = "평가소견", example = "급성 심근경색 의심, 즉시 PCI 가능 병원 이송 필요", nullable = true)
    private String assessment;

    @Schema(description = "발생일시", example = "2026-02-19T14:30:00", nullable = true)
    private LocalDateTime incidentDateTime;
}
