package com.example.aegis_be.domain.dispatch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DispatchSessionCreateRequest {

    @Schema(description = "대표자명", example = "김응급")
    @NotBlank(message = "대표자명은 필수입니다")
    private String representativeName;
}
