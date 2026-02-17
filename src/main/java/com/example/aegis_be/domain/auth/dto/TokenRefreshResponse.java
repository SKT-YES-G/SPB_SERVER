package com.example.aegis_be.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "토큰 갱신 응답")
@Getter
@Builder
public class TokenRefreshResponse {

    @Schema(
            description = "새로 발급된 Access Token (2시간 유효)",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    )
    private String accessToken;
}
