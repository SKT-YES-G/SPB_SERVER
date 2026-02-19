package com.example.aegis_be.domain.report.controller;

import com.example.aegis_be.domain.report.dto.*;
import com.example.aegis_be.domain.report.service.AmbulanceReportService;
import com.example.aegis_be.global.common.ApiResponse;
import com.example.aegis_be.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AmbulanceReport", description = "구급일지 API - AI 데이터와 구급대원 확정 데이터 분리 관리")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/dispatch/sessions/{sessionId}/report")
@RequiredArgsConstructor
@PreAuthorize("hasRole('FIRE_STATION')")
public class AmbulanceReportController {

    private final AmbulanceReportService ambulanceReportService;

    @Operation(
            summary = "구급일지 조회",
            description = """
                    **호출 주체**: 프론트엔드

                    AI 데이터 + 구급대원 확정 데이터 포함 전체 구급일지를 조회합니다.
                    구급일지가 아직 없으면 자동 생성됩니다.

                    **응답 정보**:
                    - AI 체크리스트 (68개 이진 배열)
                    - AI 바이탈 7종 (SBP, DBP, RR, PR, 체온, SpO2, 혈당)
                    - AI 텍스트 요약
                    - 구급대원 확정 체크리스트
                    - 주호소, 평가소견, 발생일시
                    - 생성/수정 시각

                    **데이터 격리**:
                    - 로그인된 소방서의 세션만 조회 가능합니다.
                    - 다른 소방서의 세션 ID로 조회 시 404 에러가 반환됩니다.

                    **인증**: 필수 (Bearer Token)
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공 - 구급일지 전체 정보 반환 (없으면 빈 일지 자동 생성 후 반환)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 - 유효하지 않은 Access Token"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "세션 없음 - 해당 ID의 세션이 존재하지 않거나 접근 권한 없음"
            )
    })
    @GetMapping
    public ApiResponse<AmbulanceReportResponse> getReport(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "출동 세션 ID", example = "1", required = true)
            @PathVariable Long sessionId) {
        return ApiResponse.success(
                ambulanceReportService.getReport(userDetails.getName(), sessionId));
    }

    // === AI용 PATCH ===

    @Operation(
            summary = "[AI] 체크리스트 업데이트",
            description = """
                    **호출 주체**: AI 서버 (FastAPI)
                    **인증**: 프론트에서 전달받은 JWT를 Authorization 헤더에 포함

                    AI가 제공하는 68개 체크리스트 이진 배열을 저장/업데이트합니다.
                    구급대원 확정 체크리스트(checklistData)에는 영향을 주지 않습니다.

                    **요청 예시**:
                    ```json
                    { "aiChecklistData": [0, 1, 0, 1, ...] }
                    ```

                    **자동 처리**: 구급일지가 없으면 자동 생성 후 업데이트

                    **인증**: 필수 (Bearer Token)
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "업데이트 성공 - 구급일지 전체 정보 반환"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 형식 오류 - 체크리스트 데이터 누락"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 - 유효하지 않은 Access Token"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "세션 없음 - 해당 ID의 세션이 존재하지 않거나 접근 권한 없음"
            )
    })
    @PatchMapping("/ai-checklist")
    public ApiResponse<AmbulanceReportResponse> updateAiChecklist(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "출동 세션 ID", example = "1", required = true)
            @PathVariable Long sessionId,
            @Valid @RequestBody AiChecklistUpdateRequest request) {
        return ApiResponse.success(
                ambulanceReportService.updateAiChecklist(userDetails.getName(), sessionId, request));
    }

    @Operation(
            summary = "[AI] 활력징후(OCR) 업데이트",
            description = """
                    **호출 주체**: AI 서버 (FastAPI)
                    **인증**: 프론트에서 전달받은 JWT를 Authorization 헤더에 포함

                    AI가 OCR로 추출한 바이탈 7종을 저장/업데이트합니다.

                    **바이탈 항목**:
                    - SBP (수축기혈압, mmHg), DBP (이완기혈압, mmHg)
                    - RR (호흡수, 회/분), PR (맥박수, 회/분)
                    - 체온 (°C), SpO2 (산소포화도, %)
                    - 혈당 (mg/dL, -1 = 측정불가)

                    **요청 예시**:
                    ```json
                    { "sbp": 120, "dbp": 80, "rr": 18, "pr": 72, "tempC": 36.5, "spO2": 98, "glucose": 100 }
                    ```

                    **자동 처리**: 구급일지가 없으면 자동 생성 후 업데이트

                    **인증**: 필수 (Bearer Token)
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "업데이트 성공 - 구급일지 전체 정보 반환"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 형식 오류 - 바이탈 데이터 누락"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 - 유효하지 않은 Access Token"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "세션 없음 - 해당 ID의 세션이 존재하지 않거나 접근 권한 없음"
            )
    })
    @PatchMapping("/vitals")
    public ApiResponse<AmbulanceReportResponse> updateVitals(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "출동 세션 ID", example = "1", required = true)
            @PathVariable Long sessionId,
            @Valid @RequestBody VitalsUpdateRequest request) {
        return ApiResponse.success(
                ambulanceReportService.updateVitals(userDetails.getName(), sessionId, request));
    }

    @Operation(
            summary = "[AI] 텍스트 요약 업데이트",
            description = """
                    **호출 주체**: AI 서버 (FastAPI)
                    **인증**: 프론트에서 전달받은 JWT를 Authorization 헤더에 포함

                    AI가 생성한 텍스트 요약을 저장/업데이트합니다.
                    번역 기록, KTAS 판정 등을 종합한 환자 상태 요약문입니다.

                    **요청 예시**:
                    ```json
                    { "summary": "60대 남성, 흉통 호소. KTAS 2등급..." }
                    ```

                    **자동 처리**: 구급일지가 없으면 자동 생성 후 업데이트

                    **인증**: 필수 (Bearer Token)
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "업데이트 성공 - 구급일지 전체 정보 반환"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 형식 오류 - 요약 텍스트 누락"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 - 유효하지 않은 Access Token"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "세션 없음 - 해당 ID의 세션이 존재하지 않거나 접근 권한 없음"
            )
    })
    @PatchMapping("/summary")
    public ApiResponse<AmbulanceReportResponse> updateSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "출동 세션 ID", example = "1", required = true)
            @PathVariable Long sessionId,
            @Valid @RequestBody SummaryUpdateRequest request) {
        return ApiResponse.success(
                ambulanceReportService.updateSummary(userDetails.getName(), sessionId, request));
    }

    // === 구급대원용 PATCH ===

    @Operation(
            summary = "[구급대원] 체크리스트 확정/수정",
            description = """
                    **호출 주체**: 프론트엔드 (구급대원)

                    구급대원이 AI 체크리스트를 검토한 후 최종 확정/수정합니다.
                    AI 체크리스트(aiChecklistData)에는 영향을 주지 않습니다.

                    **사용 흐름**:
                    1. 프론트엔드에서 AI 체크리스트(aiChecklistData)를 사전 표시
                    2. 구급대원이 검토 후 수정하여 확정
                    3. 확정된 체크리스트를 이 API로 저장

                    **요청 예시**:
                    ```json
                    { "checklistData": [0, 1, 0, 1, ...] }
                    ```

                    **자동 처리**: 구급일지가 없으면 자동 생성 후 업데이트

                    **인증**: 필수 (Bearer Token)
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "확정 성공 - 구급일지 전체 정보 반환"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 형식 오류 - 체크리스트 데이터 누락"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 - 유효하지 않은 Access Token"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "세션 없음 - 해당 ID의 세션이 존재하지 않거나 접근 권한 없음"
            )
    })
    @PatchMapping("/checklist")
    public ApiResponse<AmbulanceReportResponse> updateChecklist(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "출동 세션 ID", example = "1", required = true)
            @PathVariable Long sessionId,
            @Valid @RequestBody ChecklistUpdateRequest request) {
        return ApiResponse.success(
                ambulanceReportService.updateChecklist(userDetails.getName(), sessionId, request));
    }

    @Operation(
            summary = "[구급대원] 주호소 + 평가소견 + 발생일시 수정",
            description = """
                    **호출 주체**: 프론트엔드 (구급대원)

                    구급대원이 작성하는 주호소, 평가소견, 발생일시를 저장/업데이트합니다.
                    AI가 접근하지 않는 구급대원 전용 영역입니다.

                    **필드 설명**:
                    - chiefComplaint: 환자의 주된 호소 증상
                    - assessment: 구급대원의 현장 평가소견
                    - incidentDateTime: 사고/증상 발생일시 (날짜+시간)

                    **요청 예시**:
                    ```json
                    {
                      "chiefComplaint": "가슴 통증, 호흡 곤란",
                      "assessment": "급성 심근경색 의심, 즉시 PCI 가능 병원 이송 필요",
                      "incidentDateTime": "2026-02-19T14:30:00"
                    }
                    ```

                    **자동 처리**: 구급일지가 없으면 자동 생성 후 업데이트

                    **인증**: 필수 (Bearer Token)
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "수정 성공 - 구급일지 전체 정보 반환"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 - 유효하지 않은 Access Token"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "세션 없음 - 해당 ID의 세션이 존재하지 않거나 접근 권한 없음"
            )
    })
    @PatchMapping("/assessment")
    public ApiResponse<AmbulanceReportResponse> updateAssessment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "출동 세션 ID", example = "1", required = true)
            @PathVariable Long sessionId,
            @Valid @RequestBody AssessmentUpdateRequest request) {
        return ApiResponse.success(
                ambulanceReportService.updateAssessment(userDetails.getName(), sessionId, request));
    }
}
