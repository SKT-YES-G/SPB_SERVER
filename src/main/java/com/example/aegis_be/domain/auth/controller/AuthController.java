package com.example.aegis_be.domain.auth.controller;

import com.example.aegis_be.domain.auth.dto.*;
import com.example.aegis_be.domain.auth.service.AuthService;
import com.example.aegis_be.global.common.ApiResponse;
import com.example.aegis_be.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Auth", description = "인증 API")
@Validated
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "소방서 검색", description = "소방서명 자동완성용 검색")
    @GetMapping("/fire-stations/search")
    public ApiResponse<List<FireStationSearchResponse>> searchFireStations(
            @Parameter(description = "검색어", example = "마포") @RequestParam @NotBlank(message = "검색어는 필수입니다") String query) {
        return ApiResponse.success(authService.searchFireStations(query));
    }

    @Operation(summary = "로그인", description = "소방서명과 비밀번호로 인증 후 토큰 발급")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @Operation(summary = "토큰 갱신", description = "리프레시 토큰으로 새 액세스 토큰 발급")
    @PostMapping("/refresh")
    public ApiResponse<TokenRefreshResponse> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        return ApiResponse.success(authService.refresh(request));
    }

    @Operation(summary = "로그아웃", description = "현재 세션 로그아웃 처리")
    @PostMapping("/logout")
    @PreAuthorize("hasRole('FIRE_STATION')")
    public ApiResponse<Void> logout(@AuthenticationPrincipal CustomUserDetails userDetails) {
        authService.logout(userDetails.getName());
        return ApiResponse.success();
    }
}
