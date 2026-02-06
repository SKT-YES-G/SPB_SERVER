package com.example.aegis_be.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "토큰 갱신 요청")
@Getter
@NoArgsConstructor
public class TokenRefreshRequest {

    @Schema(
            description = "로그인 시 발급받은 Refresh Token",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "리프레시 토큰은 필수입니다")
    private String refreshToken;
}
