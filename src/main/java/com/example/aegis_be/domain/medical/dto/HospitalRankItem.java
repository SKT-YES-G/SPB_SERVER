package com.example.aegis_be.domain.medical.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "순위별 병원 항목")
@Getter
@Builder
public class HospitalRankItem {

    @Schema(description = "순위", example = "1")
    private int rank;

    @Schema(description = "종합 점수 (0.0 ~ 1.0)", example = "0.85")
    private double score;

    @Schema(description = "병원 고유 ID", example = "A1234567890")
    private String hpid;

    @Schema(description = "병원명", example = "서울대학교병원")
    private String hospitalName;

    @Schema(description = "주소", example = "서울특별시 종로구 대학로 101")
    private String address;

    @Schema(description = "대표 전화번호", example = "02-2072-2114")
    private String tel;

    @Schema(description = "응급실 전화번호", example = "02-2072-3500")
    private String emergencyTel;

    @Schema(description = "거리 (km)", example = "2.3")
    private double distanceKm;

    @Schema(description = "가용 응급실 병상 수", example = "5")
    private int availableBeds;

    @Schema(description = "병원 유형 (권역응급의료센터, 지역응급의료센터 등)", example = "권역응급의료센터")
    private String hospitalType;

    @Schema(description = "병원 위도", example = "37.5796")
    private double latitude;

    @Schema(description = "병원 경도", example = "126.999")
    private double longitude;
}
