package com.example.aegis_be.domain.translator.controller;

import com.example.aegis_be.domain.translator.dto.TranslationResponse;
import com.example.aegis_be.domain.translator.dto.TranslationSaveRequest;
import com.example.aegis_be.domain.translator.service.TranslatorService;
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

@Tag(name = "Translator", description = "번역 API - 출동 현장 다국어 실시간 번역 기록 관리")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/dispatch/sessions/{sessionId}/translations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('FIRE_STATION')")
public class TranslatorController {

    private final TranslatorService translatorService;

    @Operation(
            summary = "[AI] 번역 기록 저장",
            description = """
                    **호출 주체**: AI 서버 (FastAPI)
                    **인증**: 프론트에서 전달받은 JWT를 Authorization 헤더에 포함

                    실시간 번역 결과를 저장합니다. easyTranslation은 처음에 null로 보낼 수 있고,
                    이후 쉬운 번역이 준비되면 PATCH /{translationId}/easy로 업데이트합니다.

                    **요청 예시**:
                    ```json
                    {
                      "speaker": "환자",
                      "originalText": "I have chest pain",
                      "translatedText": "가슴이 아파요",
                      "easyTranslation": null,
                      "language": "영어"
                    }
                    ```

                    **응답**: 저장된 번역 기록 (translationId 포함 → 쉬운 번역 업데이트 시 사용)
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "저장 성공 - 생성된 번역 기록 반환"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 형식 오류 - 필수 필드(speaker, originalText, translatedText, language) 누락"
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
    @PostMapping
    public ApiResponse<TranslationResponse> saveTranslation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "출동 세션 ID", example = "1", required = true)
            @PathVariable Long sessionId,
            @Valid @RequestBody TranslationSaveRequest request) {
        return ApiResponse.success(translatorService.saveTranslation(userDetails.getName(), sessionId, request));
    }

    @Operation(
            summary = "전체 번역 기록 조회",
            description = """
                    해당 출동 세션의 전체 번역 기록을 시간순(오래된 순)으로 조회합니다.

                    **응답 정보**:
                    - 번역 기록 ID, 발화자, 원문, 번역문, 쉬운 번역문, 언어, 생성 시각

                    **정렬**: 생성 시각 기준 오래된 순 (대화 흐름 순서)

                    **데이터 격리**:
                    - 로그인된 소방서의 세션만 조회 가능합니다.
                    - 다른 소방서의 세션 ID로 조회 시 404 에러가 반환됩니다.

                    **인증**: 필수 (Bearer Token)
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공 - 번역 기록 목록 반환 (없으면 빈 배열)"
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
    public ApiResponse<List<TranslationResponse>> getTranslations(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "출동 세션 ID", example = "1", required = true)
            @PathVariable Long sessionId) {
        return ApiResponse.success(translatorService.getTranslations(userDetails.getName(), sessionId));
    }

    @Operation(
            summary = "쉬운 번역 생성",
            description = """
                    **호출 주체**: 프론트엔드

                    해당 번역 기록의 쉬운 번역문을 AI 서버에 전달하여 쉬운 번역을 생성합니다.
                    서버가 AI에 쉬운 번역을 요청하고
                    결과를 저장한 뒤 즉시 반환합니다.

                    **동작 흐름**:
                    1. 프론트엔드가 이 API 호출 (body 없음)
                    2. 스프링 서버가 AI에 쉬운 번역 생성 요청
                    3. AI 응답을 DB에 저장 + 프론트에 반환
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "생성 성공 - 쉬운 번역이 포함된 번역 기록 반환"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 - 유효하지 않은 Access Token"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "번역 기록 없음 - 해당 ID의 번역 기록이 존재하지 않거나 세션 접근 권한 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "502",
                    description = "AI 서버 오류 - AI 서버 호출 실패"
            )
    })
    @PostMapping("/{translationId}/easy")
    public ApiResponse<TranslationResponse> generateEasyTranslation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "출동 세션 ID", example = "1", required = true)
            @PathVariable Long sessionId,
            @Parameter(description = "번역 기록 ID", example = "1", required = true)
            @PathVariable Long translationId) {
        return ApiResponse.success(translatorService.generateEasyTranslation(
                userDetails.getName(), sessionId, translationId));
    }
}