package com.example.aegis_be.domain.medical.controller;

import com.example.aegis_be.domain.medical.dto.HospitalDetailResponse;
import com.example.aegis_be.domain.medical.dto.HospitalSearchRequest;
import com.example.aegis_be.domain.medical.dto.HospitalSearchResponse;
import com.example.aegis_be.domain.medical.service.MedicalMapService;
import com.example.aegis_be.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Medical Map", description = "응급 병원 추천 API - 현재 위치 기반 최적 응급 병원 검색 및 상세 조회")
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

                    **처리 흐름**:
                    - 입력된 좌표에서 각 병원까지의 실제 직선거리(Haversine)를 계산
                    - 해당 시/도의 실시간 가용병상 정보 조회
                    - 거리 + 병상 + KTAS 기반 종합 점수 산출 후 상위 15개 반환

                    **점수 산출 방식**:
                    - 거리 점수: 지수 감쇠 적용 (가까울수록 급격히 높은 점수, 약 7km마다 점수 반감)
                    - 병상 점수: 가용 병상 수 비례 (최대 20병상 기준 정규화)
                    - KTAS 점수: 중증도와 병원 유형 적합도 매칭

                    **가중치**:
                    - KTAS 입력 시: 거리 30% + 병상 20% + KTAS 50%
                    - KTAS 미입력 시: 거리 60% + 병상 40%

                    **응답 정보** (병원별):
                    - 순위(rank), 종합 점수(score, 0.0~1.0)
                    - 병원 고유 ID(hpid), 병원명, 주소, 대표 전화번호, 응급실 전화번호
                    - 현재 위치로부터의 거리(km), 가용 응급 병상 수
                    - 병원 유형(권역응급의료센터/지역응급의료센터 등), 좌표(위도/경도)

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

    @Operation(
            summary = "병원 상세 정보 조회",
            description = """
                    병원 고유 ID(hpid)로 상세 정보를 조회합니다.

                    **응답 정보**:
                    - 기본 정보: 병원 고유 ID(hpid), 병원명, 주소, 대표 전화번호, 응급실 전화번호
                    - 분류 정보: 병원 유형(권역응급의료센터/지역응급의료센터 등), 진료과목
                    - 운영 정보: 응급실 운영 여부, 입원 가능 여부, 총 병상 수
                    - 병상 현황: 응급실, 수술실, 중환자실, 신생아중환자실, 일반입원실, 외과입원실 병상 수
                    - 위치 정보: 좌표(위도/경도)

                    **인증**: 필수 (Bearer Token)
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공 - 병원 상세 정보 반환"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 - 유효하지 않은 Access Token"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "병원 없음 - 해당 ID의 병원을 찾을 수 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "502",
                    description = "외부 API 오류 - 응급의료 정보 API 호출 실패"
            )
    })
    @GetMapping("/{hpid}")
    public ApiResponse<HospitalDetailResponse> getHospitalDetail(
            @Parameter(description = "병원 고유 ID", example = "A1100017", required = true)
            @PathVariable String hpid) {
        return ApiResponse.success(medicalMapService.getHospitalDetail(hpid));
    }
}
