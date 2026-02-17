package com.example.aegis_be.domain.prektas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "구급대원 KTAS 수동 변경 요청")
@Getter
@NoArgsConstructor
public class ParamedicKtasUpdateRequest {

    @Schema(description = "구급대원 판정 KTAS 등급 (1~5)", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "KTAS 등급은 필수입니다")
    @Min(value = 1, message = "KTAS 등급은 1~5 사이여야 합니다")
    @Max(value = 5, message = "KTAS 등급은 1~5 사이여야 합니다")
    private Integer level;
}
