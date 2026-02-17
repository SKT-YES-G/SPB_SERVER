package com.example.aegis_be.domain.report.controller;

import com.example.aegis_be.domain.report.dto.*;
import com.example.aegis_be.domain.report.service.AmbulanceReportService;
import com.example.aegis_be.global.common.ApiResponse;
import com.example.aegis_be.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

    @Operation(summary = "구급일지 조회", description = """
            AI 데이터 + 구급대원 확정 데이터 포함 전체 구급일지를 조회합니다.
            구급일지가 아직 없으면 자동 생성됩니다.

            **호출 주체**: 프론트엔드
            """)
    @GetMapping
    public ApiResponse<AmbulanceReportResponse> getReport(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "출동 세션 ID") @PathVariable Long sessionId) {
        return ApiResponse.success(
                ambulanceReportService.getReport(userDetails.getName(), sessionId));
    }

    // === AI용 PATCH ===

    @Operation(summary = "[AI] 체크리스트 업데이트", description = "AI가 제공하는 68개 체크리스트를 업데이트합니다. 구급대원 확정 체크리스트에는 영향 없습니다.")
    @PatchMapping("/ai-checklist")
    public ApiResponse<AmbulanceReportResponse> updateAiChecklist(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "출동 세션 ID") @PathVariable Long sessionId,
            @Valid @RequestBody AiChecklistUpdateRequest request) {
        return ApiResponse.success(
                ambulanceReportService.updateAiChecklist(userDetails.getName(), sessionId, request));
    }

    @Operation(summary = "[AI] 활력징후(OCR) 업데이트", description = "AI가 OCR로 추출한 바이탈 7종을 업데이트합니다.")
    @PatchMapping("/vitals")
    public ApiResponse<AmbulanceReportResponse> updateVitals(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "출동 세션 ID") @PathVariable Long sessionId,
            @Valid @RequestBody VitalsUpdateRequest request) {
        return ApiResponse.success(
                ambulanceReportService.updateVitals(userDetails.getName(), sessionId, request));
    }

    @Operation(summary = "[AI] 텍스트 요약 업데이트", description = "AI가 생성한 텍스트 요약을 업데이트합니다.")
    @PatchMapping("/summary")
    public ApiResponse<AmbulanceReportResponse> updateSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "출동 세션 ID") @PathVariable Long sessionId,
            @Valid @RequestBody SummaryUpdateRequest request) {
        return ApiResponse.success(
                ambulanceReportService.updateSummary(userDetails.getName(), sessionId, request));
    }

    // === 구급대원용 PATCH ===

    @Operation(summary = "[구급대원] 체크리스트 확정/수정", description = "구급대원이 AI 체크리스트를 검토 후 확정/수정합니다. AI 체크리스트에는 영향 없습니다.")
    @PatchMapping("/checklist")
    public ApiResponse<AmbulanceReportResponse> updateChecklist(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "출동 세션 ID") @PathVariable Long sessionId,
            @Valid @RequestBody ChecklistUpdateRequest request) {
        return ApiResponse.success(
                ambulanceReportService.updateChecklist(userDetails.getName(), sessionId, request));
    }

    @Operation(summary = "[구급대원] 주호소 + 평가소견 수정", description = "구급대원이 작성하는 주호소와 평가소견을 업데이트합니다. AI가 접근하지 않는 영역입니다.")
    @PatchMapping("/assessment")
    public ApiResponse<AmbulanceReportResponse> updateAssessment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "출동 세션 ID") @PathVariable Long sessionId,
            @Valid @RequestBody AssessmentUpdateRequest request) {
        return ApiResponse.success(
                ambulanceReportService.updateAssessment(userDetails.getName(), sessionId, request));
    }
}
