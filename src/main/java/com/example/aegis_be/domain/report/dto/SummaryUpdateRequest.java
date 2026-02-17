package com.example.aegis_be.domain.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "AI 텍스트 요약 업데이트 요청")
@Getter
@NoArgsConstructor
public class SummaryUpdateRequest {

    @Schema(description = "AI 텍스트 요약", example = "환자는 가슴 통증을 호소하며...")
    @NotBlank(message = "요약 텍스트는 필수입니다")
    private String summary;
}
