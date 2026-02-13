package com.example.aegis_be.domain.medical.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "병원 상세 정보 응답")
@Getter
@Builder
public class HospitalDetailResponse {

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

    @Schema(description = "병원 유형", example = "권역응급의료센터")
    private String hospitalType;

    @Schema(description = "응급실 운영 여부", example = "Y")
    private String emergencyOperating;

    @Schema(description = "입원실 가용 여부", example = "Y")
    private String hospitalizationAvailable;

    @Schema(description = "병상 수", example = "500")
    private String bedCount;

    @Schema(description = "진료과목", example = "내과,외과,소아과")
    private String departments;

    @Schema(description = "병원 위도", example = "37.5796")
    private double latitude;

    @Schema(description = "병원 경도", example = "126.999")
    private double longitude;

    @Schema(description = "병상 현황")
    private BedInfo bedInfo;

    @Schema(description = "병상 현황 정보")
    @Getter
    @Builder
    public static class BedInfo {

        @Schema(description = "응급실 병상 수", example = "10")
        private int emergencyBeds;

        @Schema(description = "수술실 수", example = "5")
        private int operatingRooms;

        @Schema(description = "중환자실 병상 수", example = "8")
        private int icuBeds;

        @Schema(description = "신생아중환자실 병상 수", example = "3")
        private int neonatalIcuBeds;

        @Schema(description = "일반입원실 병상 수", example = "200")
        private int generalBeds;

        @Schema(description = "외과입원실 병상 수", example = "50")
        private int surgicalBeds;
    }
}
