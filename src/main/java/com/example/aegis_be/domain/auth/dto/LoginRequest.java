package com.example.aegis_be.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "로그인 요청")
@Getter
@NoArgsConstructor
public class LoginRequest {

    @Schema(
            description = "소방서명 (검색 API로 확인 가능)",
            example = "마포소방서",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "소방서명은 필수입니다")
    private String name;

    @Schema(
            description = "비밀번호",
            example = "string",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "비밀번호는 필수입니다")
    private String password;
}