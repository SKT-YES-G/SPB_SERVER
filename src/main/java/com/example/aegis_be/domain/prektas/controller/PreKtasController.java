package com.example.aegis_be.domain.prektas.controller;

import com.example.aegis_be.domain.prektas.dto.AiKtasUpdateRequest;
import com.example.aegis_be.domain.prektas.dto.KtasSyncToggleRequest;
import com.example.aegis_be.domain.prektas.dto.ParamedicKtasUpdateRequest;
import com.example.aegis_be.domain.prektas.dto.PreKtasResponse;
import com.example.aegis_be.domain.prektas.service.PreKtasService;
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

@Tag(name = "PreKTAS", description = "PreKTAS API - AI KTAS 및 구급대원 KTAS 판정 등급 관리")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/dispatch/sessions/{sessionId}/ktas")
@RequiredArgsConstructor
@PreAuthorize("hasRole('FIRE_STATION')")
public class PreKtasController {

    private final PreKtasService preKtasService;

    @Operation(
            summary = "[AI] KTAS 등급 저장/업데이트",
            description = """
                    **호출 주체**: AI 서버 (FastAPI)
                    **인증**: 프론트에서 전달받은 JWT를 Authorization 헤더에 포함

                    AI가 판정한 KTAS 등급(1~5)과 판정 근거를 저장합니다.

                    **동기화 동작**:
                    - 동기화 ON: AI KTAS 변경 → 구급대원 KTAS도 자동 반영
                    - 동기화 OFF: AI KTAS만 변경, 구급대원 KTAS 유지

                    **자동 처리**: KTAS 정보 없으면 자동 생성, 이벤트 로그 자동 기록

                    **요청 예시**:
                    ```json
                    { "level": 3, "reasoning": "흉통 및 좌측 팔 저림으로 KTAS 3 판정" }
                    ```
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "저장 성공 - AI/구급대원 KTAS, 동기화 상태 포함 응답"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 형식 오류 - KTAS 등급 누락 또는 범위 초과 (1~5)"
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
    @PutMapping("/ai")
    public ApiResponse<PreKtasResponse> updateAiKtas(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "출동 세션 ID", example = "1", required = true)
            @PathVariable Long sessionId,
            @Valid @RequestBody AiKtasUpdateRequest request) {
        return ApiResponse.success(preKtasService.updateAiKtas(userDetails.getName(), sessionId, request));
    }

    @Operation(
            summary = "구급대원 KTAS 수동 변경",
            description = """
                    구급대원이 직접 판정한 KTAS 등급으로 변경합니다.

                    **동기화 자동 해제**:
                    - 수동 변경 시 동기화가 자동으로 OFF 됩니다.
                    - 이후 AI KTAS가 변경되어도 구급대원 KTAS에 영향을 주지 않습니다.
                    - 동기화를 다시 원하면 KTAS 동기화 토글 API를 호출하세요.

                    **자동 처리 항목**:
                    - KTAS 정보가 없으면 최초 호출 시 자동 생성 (lazy 생성)
                    - PARAMEDIC_KTAS_CHANGE 이벤트 로그 자동 기록
                    - 동기화가 ON이었을 경우 SYNC_TOGGLE 이벤트 로그 추가 기록

                    **인증**: 필수 (Bearer Token)
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "변경 성공 - 업데이트된 KTAS 정보 반환 (synced=false)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 형식 오류 - KTAS 등급 누락 또는 범위 초과 (1~5)"
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
    @PutMapping("/paramedic")
    public ApiResponse<PreKtasResponse> updateParamedicKtas(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "출동 세션 ID", example = "1", required = true)
            @PathVariable Long sessionId,
            @Valid @RequestBody ParamedicKtasUpdateRequest request) {
        return ApiResponse.success(preKtasService.updateParamedicKtas(userDetails.getName(), sessionId, request));
    }

    @Operation(
            summary = "KTAS 동기화 ON/OFF 토글",
            description = """
                    **호출 주체**: 프론트엔드 (구급대원)

                    AI KTAS와 구급대원 KTAS 간의 동기화를 ON/OFF 합니다.

                    **ON 전환 시**:
                    - AI KTAS가 있으면 구급대원 KTAS가 AI 값으로 즉시 동기화
                    - AI KTAS가 없으면 동기화 상태만 ON (이후 AI KTAS 수신 시 자동 반영)

                    **OFF 전환 시**:
                    - AI KTAS 변경이 구급대원 KTAS에 반영되지 않음

                    **자동 처리**: KTAS 정보 없으면 자동 생성, SYNC_TOGGLE 이벤트 로그 기록
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "토글 성공 - 업데이트된 KTAS 정보 반환"
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
    @PatchMapping("/sync")
    public ApiResponse<PreKtasResponse> toggleSync(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "출동 세션 ID", example = "1", required = true)
            @PathVariable Long sessionId,
            @Valid @RequestBody KtasSyncToggleRequest request) {
        return ApiResponse.success(preKtasService.toggleSync(userDetails.getName(), sessionId, request));
    }

    @Operation(
            summary = "KTAS 정보 조회",
            description = """
                    해당 출동 세션의 KTAS 전체 정보를 조회합니다.

                    **응답 정보**:
                    - AI 판정 KTAS 등급 및 판정 근거
                    - 구급대원 판정 KTAS 등급
                    - 동기화 상태 (synced)
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
                    description = "조회 성공 - KTAS 상세 정보 반환"
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
    public ApiResponse<PreKtasResponse> getKtas(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "출동 세션 ID", example = "1", required = true)
            @PathVariable Long sessionId) {
        return ApiResponse.success(preKtasService.getKtas(userDetails.getName(), sessionId));
    }
}
