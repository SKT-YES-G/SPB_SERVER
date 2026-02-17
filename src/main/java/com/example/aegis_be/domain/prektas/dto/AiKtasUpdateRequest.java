package com.example.aegis_be.domain.prektas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "AI KTAS 저장/업데이트 요청")
@Getter
@NoArgsConstructor
public class AiKtasUpdateRequest {

    @Schema(description = "AI 판정 KTAS 등급 (1~5)", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "AI KTAS 등급은 필수입니다")
    @Min(value = 1, message = "KTAS 등급은 1~5 사이여야 합니다")
    @Max(value = 5, message = "KTAS 등급은 1~5 사이여야 합니다")
    private Integer level;

    @Schema(description = "AI 판정 근거", example = "환자 호흡곤란 및 의식 저하로 KTAS 3 판정")
    private String reasoning;
}
