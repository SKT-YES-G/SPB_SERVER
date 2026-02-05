package com.example.aegis_be.domain.dispatch.controller;

import com.example.aegis_be.domain.dispatch.dto.DispatchSessionCreateRequest;
import com.example.aegis_be.domain.dispatch.dto.DispatchSessionResponse;
import com.example.aegis_be.domain.dispatch.service.DispatchService;
import com.example.aegis_be.global.common.ApiResponse;
import com.example.aegis_be.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Dispatch", description = "출동 세션 API")
@RestController
@RequestMapping("/api/dispatch/sessions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('FIRE_STATION')")
public class DispatchController {

    private final DispatchService dispatchService;

    @Operation(summary = "출동 세션 생성", description = "대표자명을 입력하여 새 출동 세션을 시작. 날짜/시간은 자동 기록.")
    @PostMapping
    public ApiResponse<DispatchSessionResponse> createSession(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody DispatchSessionCreateRequest request) {
        return ApiResponse.success(dispatchService.createSession(userDetails.getName(), request));
    }

    @Operation(summary = "출동 세션 조회", description = "세션 ID로 출동 세션 상세 정보 조회")
    @GetMapping("/{sessionId}")
    public ApiResponse<DispatchSessionResponse> getSession(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sessionId) {
        return ApiResponse.success(dispatchService.getSession(userDetails.getName(), sessionId));
    }

    @Operation(summary = "활성 세션 목록", description = "현재 출동 중인 세션 목록 조회. 혹시 몰라서 추가.")
    @GetMapping("/active")
    public ApiResponse<List<DispatchSessionResponse>> getActiveSessions(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.success(dispatchService.getActiveSessions(userDetails.getName()));
    }

    @Operation(summary = "전체 세션 목록", description = "모든 출동 세션 목록 조회 (완료 포함)")
    @GetMapping
    public ApiResponse<List<DispatchSessionResponse>> getAllSessions(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.success(dispatchService.getAllSessions(userDetails.getName()));
    }

    @Operation(summary = "출동 완료", description = "출동 세션을 완료 처리. 완료 시각이 자동 기록.")
    @PatchMapping("/{sessionId}/complete")
    public ApiResponse<DispatchSessionResponse> completeSession(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sessionId) {
        return ApiResponse.success(dispatchService.completeSession(userDetails.getName(), sessionId));
    }
}