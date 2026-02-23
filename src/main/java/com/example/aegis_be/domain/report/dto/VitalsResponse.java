package com.example.aegis_be.domain.report.dto;

import com.example.aegis_be.domain.report.entity.AmbulanceReport;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "활력징후(OCR) 응답")
@Getter
@Builder
public class VitalsResponse {

    @Schema(description = "구급일지 ID", example = "1")
    private Long reportId;

    @Schema(description = "출동 세션 ID", example = "1")
    private Long sessionId;

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

    public static VitalsResponse from(AmbulanceReport report) {
        return VitalsResponse.builder()
                .reportId(report.getId())
                .sessionId(report.getDispatchSession().getId())
                .sbp(report.getSbp())
                .dbp(report.getDbp())
                .rr(report.getRr())
                .pr(report.getPr())
                .tempC(report.getTempC())
                .spO2(report.getSpO2())
                .glucose(report.getGlucose())
                .build();
    }
}
