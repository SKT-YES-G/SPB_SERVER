package com.example.aegis_be.domain.dispatch.controller;

import com.example.aegis_be.domain.dispatch.dto.DispatchSessionCreateRequest;
import com.example.aegis_be.domain.dispatch.dto.DispatchSessionResponse;
import com.example.aegis_be.domain.dispatch.service.DispatchService;
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

import java.util.List;

@Tag(name = "Dispatch", description = "출동 세션 API - 소방서 출동 세션의 생성, 조회, 완료 처리를 관리")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/dispatch/sessions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('FIRE_STATION')")
public class DispatchController {

    private final DispatchService dispatchService;

    @Operation(
            summary = "출동 세션 생성",
            description = """
                    새로운 출동 세션을 생성합니다.

                    **자동 처리 항목**:
                    - 출동 시각(dispatchedAt): 서버 시간 기준 자동 기록 (KST)
                    - 상태(status): ACTIVE로 자동 설정
                    - 소방서 정보: 로그인된 소방서로 자동 연결

                    **데이터 격리**:
                    - 생성된 세션은 해당 소방서에서만 조회/관리 가능합니다.

                    **인증**: 필수 (Bearer Token)
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "세션 생성 성공 - 생성된 세션 정보 반환"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 형식 오류 - 대표자명 누락"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 - 유효하지 않은 Access Token"
            )
    })
    @PostMapping
    public ApiResponse<DispatchSessionResponse> createSession(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody DispatchSessionCreateRequest request) {
        return ApiResponse.success(dispatchService.createSession(userDetails.getName(), request));
    }

    @Operation(
            summary = "출동 세션 조회",
            description = """
                    세션 ID로 특정 출동 세션의 상세 정보를 조회합니다.

                    **응답 정보**:
                    - 세션 ID, 소방서명, 대표자명
                    - 상태(ACTIVE/COMPLETED)
                    - 출동 시각, 완료 시각(완료된 경우)

                    **데이터 격리**:
                    - 로그인된 소방서의 세션만 조회 가능합니다.
                    - 다른 소방서의 세션 ID로 조회 시 404 에러가 반환됩니다.

                    **인증**: 필수 (Bearer Token)
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공 - 세션 상세 정보 반환"
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
    @GetMapping("/{sessionId}")
    public ApiResponse<DispatchSessionResponse> getSession(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "조회할 세션 ID", example = "1", required = true)
            @PathVariable Long sessionId) {
        return ApiResponse.success(dispatchService.getSession(userDetails.getName(), sessionId));
    }

    @Operation(
            summary = "활성 세션 목록 조회",
            description = """
                    현재 출동 중(ACTIVE)인 세션 목록을 조회합니다.

                    **필터 조건**: status = ACTIVE

                    **정렬**: 출동 시각 기준 최신순

                    **사용 목적**:
                    - 현재 진행 중인 출동 현황 대시보드
                    - 완료 처리가 필요한 세션 확인

                    **인증**: 필수 (Bearer Token)
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공 - 활성 세션 목록 반환 (없으면 빈 배열)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 - 유효하지 않은 Access Token"
            )
    })
    @GetMapping("/active")
    public ApiResponse<List<DispatchSessionResponse>> getActiveSessions(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.success(dispatchService.getActiveSessions(userDetails.getName()));
    }

    @Operation(
            summary = "전체 세션 목록 조회",
            description = """
                    해당 소방서의 모든 출동 세션을 조회합니다. (ACTIVE + COMPLETED)

                    **정렬**: 출동 시각 기준 최신순

                    **사용 목적**:
                    - 출동 이력 조회
                    - 출동 통계 분석

                    **인증**: 필수 (Bearer Token)
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공 - 전체 세션 목록 반환 (없으면 빈 배열)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 - 유효하지 않은 Access Token"
            )
    })
    @GetMapping
    public ApiResponse<List<DispatchSessionResponse>> getAllSessions(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.success(dispatchService.getAllSessions(userDetails.getName()));
    }

    @Operation(
            summary = "출동 완료 처리 변경",
            description = """
                    출동 세션을 완료 상태로 변경합니다.

                    **자동 처리 항목**:
                    - 완료 시각(completedAt): 서버 시간 기준 자동 기록 (KST)
                    - 상태(status): ACTIVE → COMPLETED로 변경

                    **주의사항**:
                    - 이미 완료된 세션은 다시 완료 처리할 수 없습니다.
                    - 완료 처리는 취소할 수 없습니다.

                    **인증**: 필수 (Bearer Token)
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "완료 처리 성공 - 업데이트된 세션 정보 반환"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "처리 불가 - 이미 완료된 세션"
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
    @PatchMapping("/{sessionId}/complete")
    public ApiResponse<DispatchSessionResponse> completeSession(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "완료 처리할 세션 ID", example = "1", required = true)
            @PathVariable Long sessionId) {
        return ApiResponse.success(dispatchService.completeSession(userDetails.getName(), sessionId));
    }
}