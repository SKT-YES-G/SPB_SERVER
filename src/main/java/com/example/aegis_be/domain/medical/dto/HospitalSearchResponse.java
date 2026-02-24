package com.example.aegis_be.domain.medical.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Schema(description = "응급 병원 검색 응답")
@Getter
@Builder
public class HospitalSearchResponse {

    @Schema(description = "검색된 병원 수", example = "10")
    private int totalCount;

    @Schema(description = "KTAS 기반 점수 적용 여부", example = "true")
    private boolean ktasApplied;

    @Schema(description = "AI 추천 진료과 목록", example = "[\"정형외과\", \"신경외과\"]")
    private List<String> recommendedDepartments;

    @Schema(description = "순위별 병원 목록")
    private List<HospitalRankItem> hospitals;
}
