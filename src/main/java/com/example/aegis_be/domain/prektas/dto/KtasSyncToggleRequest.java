package com.example.aegis_be.domain.prektas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "KTAS 동기화 ON/OFF 토글 요청")
@Getter
@NoArgsConstructor
public class KtasSyncToggleRequest {

    @Schema(description = "동기화 활성화 여부 (true: ON, false: OFF)", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "동기화 상태는 필수입니다")
    private Boolean synced;
}
