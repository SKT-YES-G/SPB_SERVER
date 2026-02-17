package com.example.aegis_be.domain.eventlog.controller;

import com.example.aegis_be.domain.eventlog.dto.EventLogResponse;
import com.example.aegis_be.domain.eventlog.dto.EventLogSaveRequest;
import com.example.aegis_be.domain.eventlog.service.EventLogService;
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
            summary = "[AI] 분석 텍스트 저장",
            description = """
                    AI 서버가 분석한 텍스트를 이벤트 로그로 저장합니다.

                    **호출 주체**: AI 서버 (FastAPI)
                    **인증**: 프론트에서 전달받은 JWT를 Authorization 헤더에 포함

                    **동작**:
                    - description에 AI 분석 텍스트(줄글)를 담아 전송
                    - 서버에서 자동으로 KEYWORD_DETECTED 타입 + 현재 시각 기록

                    **요청 예시**:
                    ```json
                    {
                      "description": "환자가 흉통을 호소하며 좌측 팔 저림 증상 동반..."
                    }
                    ```
                    """
    )
    @PostMapping
    public ApiResponse<EventLogResponse> saveLog(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "출동 세션 ID") @PathVariable Long sessionId,
            @Valid @RequestBody EventLogSaveRequest request) {
        return ApiResponse.success(eventLogService.saveLog(userDetails.getName(), sessionId, request));
    }

    @Operation(
            summary = "전체 이벤트 로그 조회",
            description = """
                    해당 출동 세션의 전체 이벤트 타임라인을 시간순으로 조회합니다.

                    **호출 주체**: 프론트엔드

                    **포함되는 이벤트 유형**:
                    - SESSION_START / SESSION_END: 세션 시작/종료 (자동)
                    - AI_KTAS_CHANGE / PARAMEDIC_KTAS_CHANGE: KTAS 등급 변경 (자동)
                    - SYNC_TOGGLE: 동기화 ON/OFF (자동)
                    - KEYWORD_DETECTED: AI 분석 텍스트 (AI 서버가 POST)

                    **정렬**: 생성 시각 오래된 순 (활동 발생 순서)
                    """
    )
    @GetMapping
    public ApiResponse<List<EventLogResponse>> getLogs(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "출동 세션 ID") @PathVariable Long sessionId) {
        return ApiResponse.success(eventLogService.getLogs(userDetails.getName(), sessionId));
    }
}
