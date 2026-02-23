package com.example.aegis_be.domain.medical.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "응급 병원 검색 요청")
@Getter
@NoArgsConstructor
public class HospitalSearchRequest {

    @Schema(description = "현재 위치 위도", example = "37.5665")
    @NotNull(message = "위도는 필수입니다")
    @Min(value = 33, message = "유효하지 않은 위도입니다")
    @Max(value = 39, message = "유효하지 않은 위도입니다")
    private Double latitude;

    @Schema(description = "현재 위치 경도", example = "126.978")
    @NotNull(message = "경도는 필수입니다")
    @Min(value = 124, message = "유효하지 않은 경도입니다")
    @Max(value = 132, message = "유효하지 않은 경도입니다")
    private Double longitude;

    @Schema(description = "KTAS 등급 (1~5, 선택)", example = "3")
    @Min(value = 1, message = "KTAS 등급은 1~5 사이여야 합니다")
    @Max(value = 5, message = "KTAS 등급은 1~5 사이여야 합니다")
    private Integer ktasLevel;

    @Schema(description = "AI 팀 추천 진료과 목록", example = "[\"내과\", \"외과\"]")
    private List<String> departments;

    @Schema(description = "출동 세션 ID (진료과 자동 추천 시 필요)", example = "1")
    private Long sessionId;
}
