package com.example.aegis_be.domain.eventlog.controller;

import com.example.aegis_be.domain.eventlog.dto.EventLogResponse;
import com.example.aegis_be.domain.eventlog.service.EventLogService;
import com.example.aegis_be.global.common.ApiResponse;
import com.example.aegis_be.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "EventLog", description = "이벤트 로그 API - 출동 세션 내 활동 타임라인")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/dispatch/sessions/{sessionId}/logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('FIRE_STATION')")
public class EventLogController {

    private final EventLogService eventLogService;

    @Operation(
            summary = "전체 이벤트 로그 조회",
            description = """
                    해당 출동 세션의 전체 이벤트 타임라인을 시간순으로 조회합니다.

                    **호출 주체**: 프론트엔드

                    **포함되는 이벤트 유형**:
                    - SESSION_START / SESSION_END: 세션 시작/종료 (자동)
                    - AI_KTAS_CHANGE: AI 중증도 판단 등급 변경 (자동)
                    - PARAMEDIC_KTAS_CHANGE: 구급대원 중증도 등급 수동 변경 (동기화 ON이면 미기록)
                    - SYNC_TOGGLE: AI 동기화 ON/OFF (자동)
                    - AI_REASONING_SAVED: AI 판단근거 저장 (stage 변경 시 자동)

                    **정렬**: 생성 시각 오래된 순 (활동 발생 순서)

                    **인증**: 필수 (Bearer Token)
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공 - 이벤트 로그 목록 반환 (없으면 빈 배열)"
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
    public ApiResponse<List<EventLogResponse>> getLogs(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "출동 세션 ID") @PathVariable Long sessionId) {
        return ApiResponse.success(eventLogService.getLogs(userDetails.getName(), sessionId));
    }
}
