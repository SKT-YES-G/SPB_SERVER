package com.example.aegis_be.domain.auth.controller;

import com.example.aegis_be.domain.auth.dto.*;
import com.example.aegis_be.domain.auth.service.AuthService;
import com.example.aegis_be.global.common.ApiResponse;
import com.example.aegis_be.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Auth", description = "인증 API - 소방서 로그인, 토큰 관리, 소방서 검색 기능")
@Validated
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "소방서 검색",
            description = """
                    소방서명을 키워드로 검색하여 일치하는 소방서 목록을 반환합니다.

                    **사용 목적**: 로그인 화면에서 소방서명 자동완성(autocomplete) 기능에 활용됩니다.

                    **검색 방식**: 입력된 키워드가 소방서명에 포함되어 있으면 검색 결과에 포함됩니다. (LIKE '%keyword%')

                    **인증**: 불필요 (공개 API)
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "검색 성공 - 일치하는 소방서 목록 반환 (결과 없으면 빈 배열)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "검색어 누락 - query 파라미터가 비어있거나 공백만 있는 경우"
            )
    })
    @GetMapping("/fire-stations/search")
    public ApiResponse<List<FireStationSearchResponse>> searchFireStations(
            @Parameter(
                    description = "검색할 소방서명 키워드 (최소 1자 이상)",
                    example = "마포",
                    required = true
            )
            @RequestParam @NotBlank(message = "검색어는 필수입니다") String query) {
        return ApiResponse.success(authService.searchFireStations(query));
    }

    @Operation(
            summary = "로그인",
            description = """
                    소방서명과 비밀번호로 인증하여 JWT 토큰을 발급받습니다.

                    **토큰 정책**:
                    - Access Token: 1시간 유효, API 요청 시 Authorization 헤더에 Bearer 토큰으로 전달
                    - Refresh Token: 2주 유효, Access Token 만료 시 갱신용으로 사용

                    **보안**:
                    - Refresh Token은 서버 Redis에 저장되어 관리됩니다.
                    - 동일 소방서로 재로그인 시 기존 Refresh Token은 무효화됩니다.

                    **인증**: 불필요 (공개 API)
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공 - Access Token, Refresh Token, 소방서 정보 반환"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 형식 오류 - 필수 필드 누락 또는 빈 값"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 - 소방서명 또는 비밀번호 불일치"
            )
    })
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @Operation(
            summary = "토큰 갱신",
            description = """
                    Refresh Token을 사용하여 새로운 Access Token을 발급받습니다.

                    **사용 시점**: Access Token이 만료되었을 때 (401 응답 수신 시)

                    **주의사항**:
                    - Refresh Token 자체는 갱신되지 않습니다. (기존 Refresh Token 계속 사용)
                    - Refresh Token이 만료되었거나 무효화된 경우 재로그인이 필요합니다.

                    **인증**: 불필요 (Refresh Token으로 검증)
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "갱신 성공 - 새로운 Access Token 반환"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 형식 오류 - Refresh Token 누락"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "갱신 실패 - Refresh Token이 만료되었거나 유효하지 않음"
            )
    })
    @PostMapping("/refresh")
    public ApiResponse<TokenRefreshResponse> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        return ApiResponse.success(authService.refresh(request));
    }

    @Operation(
            summary = "로그아웃",
            description = """
                    현재 로그인된 세션을 로그아웃 처리합니다.

                    **처리 내용**:
                    - 서버에 저장된 Refresh Token을 삭제합니다.
                    - 이후 해당 Refresh Token으로 토큰 갱신이 불가능합니다.

                    **클라이언트 처리**:
                    - 클라이언트에서 저장된 Access Token과 Refresh Token을 삭제해야 합니다.

                    **인증**: 필수 (Bearer Token)
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그아웃 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 - 유효하지 않은 Access Token"
            )
    })
    @PostMapping("/logout")
    @PreAuthorize("hasRole('FIRE_STATION')")
    public ApiResponse<Void> logout(@AuthenticationPrincipal CustomUserDetails userDetails) {
        authService.logout(userDetails.getName());
        return ApiResponse.success();
    }
}