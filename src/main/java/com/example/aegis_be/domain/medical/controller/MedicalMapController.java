package com.example.aegis_be.domain.medical.controller;

import com.example.aegis_be.domain.medical.dto.HospitalSearchRequest;
import com.example.aegis_be.domain.medical.dto.HospitalSearchResponse;
import com.example.aegis_be.domain.medical.service.MedicalMapService;
import com.example.aegis_be.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Medical Map", description = "응급 병원 추천 API - 현재 위치 기반 최적 응급 병원 검색")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/medical/hospitals")
@RequiredArgsConstructor
@PreAuthorize("hasRole('FIRE_STATION')")
public class MedicalMapController {

    private final MedicalMapService medicalMapService;

    @Operation(
            summary = "응급 병원 검색",
            description = """
                    현재 위치 좌표를 기반으로 최적의 응급 병원 목록을 검색합니다.

                    **필터링**:
                    - Filter 0: 응급실 운영 중인 병원만 포함
                    - Filter 1: 진료과 중 하나라도 일치하는 병원만 포함 (departments 입력 시)
                    진료과는 프론트가 요청 때 함께 넣어서 입력해야 합니다.

                    **처리 흐름**:
                    - 좌표 기반 인접 시/도 2개를 자동 판별하여 병원 목록 통합 조회
                    - 입력된 좌표에서 각 병원까지의 실제 직선거리(Haversine)를 계산
                    - 해당 시/도들의 실시간 가용병상 정보 조회
                    - 거리 + 병상 + KTAS 기반 종합 점수 산출 후 상위 15개 반환

                    **점수 산출 방식**:
                    - 거리 점수: 지수 감쇠 적용 (가까울수록 급격히 높은 점수, 약 7km마다 점수 반감)
                    - 병상 점수: 가용병상(응급+중환자+입원) 5병상 이상이면 만점, 0병상은 0점
                    - KTAS 점수: 중증도와 병원 유형 적합도 매칭

                    **가중치**:
                    - KTAS 입력 시: 거리 50% + KTAS 35% + 병상 15%
                    - KTAS 미입력 시: 거리 80% + 병상 20%

                    **응답 필드 (Response Fields)**:
                    - rank (순위)
                    - score (종합 점수, 0.0~1.0)
                    - hpid (병원 고유 ID)
                    - hospitalName (병원명)
                    - distanceKm (거리, km)
                    - emergencyBeds (응급실 가용 병상 수)
                    - icuBeds (중환자실 가용 병상 수)
                    - inpatientBeds (입원실 가용 병상 수)
                    - availableBeds (가용 병상 수 합계 = 응급 + 중환자 + 입원)
                    - hospitalType (병원 유형)
                    - departments (진료과목)
                    - address (주소)
                    - mainTel (대표 전화번호)
                    - emergencyTel (응급실 전화번호)
                    - dutyTel (당직 전화번호) : 미제공 경우 많음
                    - latitude (위도)
                    - longitude (경도)

                    **인증**: 필수 (Bearer Token)
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "검색 성공 - 점수순 병원 목록 반환 (최대 15개)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 형식 오류 - 좌표 누락 또는 유효하지 않은 값"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 - 유효하지 않은 Access Token"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "502",
                    description = "외부 API 오류 - 응급의료 정보 API 호출 실패"
            )
    })
    @PostMapping("/search")
    public ApiResponse<HospitalSearchResponse> searchHospitals(
            @Valid @RequestBody HospitalSearchRequest request) {
        return ApiResponse.success(medicalMapService.searchHospitals(request));
    }
}