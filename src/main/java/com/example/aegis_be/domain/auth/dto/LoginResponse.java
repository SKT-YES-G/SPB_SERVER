package com.example.aegis_be.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "로그인 응답")
@Getter
@Builder
public class LoginResponse {

    @Schema(
            description = "Access Token (2시간 유효) - API 요청 시 Authorization: Bearer {token} 헤더에 포함",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    )
    private String accessToken;

    @Schema(
            description = "Refresh Token (2주 유효) - Access Token 만료 시 갱신용",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    )
    private String refreshToken;

    @Schema(description = "로그인한 소방서 정보")
    private FireStationInfo fireStation;

    @Schema(description = "소방서 정보")
    @Getter
    @Builder
    public static class FireStationInfo {
        @Schema(description = "소방서명", example = "마포소방서")
        private String name;
    }
}
