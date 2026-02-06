package com.example.aegis_be.domain.dispatch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "출동 세션 생성 요청")
@Getter
@NoArgsConstructor
public class DispatchSessionCreateRequest {

    @Schema(
            description = "출동 대표자명 (출동 책임자 또는 팀장)",
            example = "김응급",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "대표자명은 필수입니다")
    private String representativeName;
}
