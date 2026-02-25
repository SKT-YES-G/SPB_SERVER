package com.example.aegis_be.domain.eventlog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "이벤트 로그 직접 입력 요청")
@Getter
@NoArgsConstructor
public class EventLogCreateRequest {

    @Schema(description = "사용자가 입력한 텍스트", example = "환자 의식 저하 확인", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "입력 텍스트는 필수입니다")
    private String description;
}
