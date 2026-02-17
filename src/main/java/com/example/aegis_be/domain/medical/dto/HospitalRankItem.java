package com.example.aegis_be.domain.medical.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "순위별 병원 항목")
@Getter
@Builder(toBuilder = true)
public class HospitalRankItem {

    @Schema(description = "순위", example = "1")
    private int rank;

    @Schema(description = "종합 점수 (0.0 ~ 1.0)", example = "0.85")
    private double score;

    @Schema(description = "병원 고유 ID", example = "A1234567890")
    private String hpid;

    @Schema(description = "병원명", example = "서울대학교병원")
    private String hospitalName;

    @Schema(description = "거리 (km)", example = "2.3")
    private double distanceKm;

    @Schema(description = "응급실 가용 병상 수", example = "5")
    private int emergencyBeds;

    @Schema(description = "중환자실 가용 병상 수", example = "3")
    private int icuBeds;

    @Schema(description = "입원실 가용 병상 수", example = "10")
    private int inpatientBeds;

    @Schema(description = "가용 병상 수 (응급실 + 중환자실 + 입원실)", example = "18")
    private int availableBeds;

    @Schema(description = "병원 유형 (권역응급의료센터, 지역응급의료센터 등)", example = "권역응급의료센터")
    private String hospitalType;

    @Schema(description = "진료과목", example = "내과,외과,소아청소년과")
    private String departments;

    @Schema(description = "주소", example = "서울특별시 종로구 대학로 101")
    private String address;

    @Schema(description = "대표 전화번호", example = "02-2072-2114")
    private String mainTel;

    @Schema(description = "응급실 전화번호", example = "02-2072-3500")
    private String emergencyTel;

    @Schema(description = "당직 전화번호", example = "02-2072-3000")
    private String dutyTel;

    @Schema(description = "병원 위도", example = "37.5796")
    private double latitude;

    @Schema(description = "병원 경도", example = "126.999")
    private double longitude;
}
