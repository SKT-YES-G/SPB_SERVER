package com.example.aegis_be.domain.report.dto;

import com.example.aegis_be.domain.report.entity.AmbulanceReport;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "구급일지 응답")
@Getter
@Builder
public class AmbulanceReportResponse {

    @Schema(description = "구급일지 ID", example = "1")
    private Long reportId;

    @Schema(description = "출동 세션 ID", example = "1")
    private Long sessionId;

    // === AI 제공 ===

    @Schema(description = "AI 체크리스트 (68개 이진 배열)", nullable = true)
    private List<Integer> aiChecklistData;

    @Schema(description = "수축기혈압 (mmHg)", example = "120", nullable = true)
    private Integer sbp;

    @Schema(description = "이완기혈압 (mmHg)", example = "80", nullable = true)
    private Integer dbp;

    @Schema(description = "호흡수 (회/분)", example = "18", nullable = true)
    private Integer rr;

    @Schema(description = "맥박수 (회/분)", example = "72", nullable = true)
    private Integer pr;

    @Schema(description = "체온 (°C)", example = "36.5", nullable = true)
    private Double tempC;

    @Schema(description = "산소포화도 (%)", example = "98", nullable = true)
    private Integer spO2;

    @Schema(description = "혈당 (mg/dL, -1 = 측정불가)", example = "100", nullable = true)
    private Integer glucose;

    @Schema(description = "AI 텍스트 요약", nullable = true)
    private String summary;

    // === 구급대원 확정 ===

    @Schema(description = "구급대원 확정 체크리스트 (68개 이진 배열)", nullable = true)
    private List<Integer> checklistData;

    @Schema(description = "주호소", example = "가슴 통증, 호흡 곤란", nullable = true)
    private String chiefComplaint;

    @Schema(description = "평가소견", example = "급성 심근경색 의심", nullable = true)
    private String assessment;

    @Schema(description = "발생일시", example = "2026-02-19T14:30:00", nullable = true)
    private LocalDateTime incidentDateTime;

    // === 시각 ===

    @Schema(description = "생성시각 (KST)", example = "2024-01-15T14:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정시각 (KST)", example = "2024-01-15T16:45:00", nullable = true)
    private LocalDateTime updatedAt;

    public static AmbulanceReportResponse from(AmbulanceReport report) {
        return AmbulanceReportResponse.builder()
                .reportId(report.getId())
                .sessionId(report.getDispatchSession().getId())
                .aiChecklistData(report.getAiChecklistData())
                .sbp(report.getSbp())
                .dbp(report.getDbp())
                .rr(report.getRr())
                .pr(report.getPr())
                .tempC(report.getTempC())
                .spO2(report.getSpO2())
                .glucose(report.getGlucose())
                .summary(report.getSummary())
                .checklistData(report.getChecklistData())
                .chiefComplaint(report.getChiefComplaint())
                .assessment(report.getAssessment())
                .incidentDateTime(report.getIncidentDateTime())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }
}
