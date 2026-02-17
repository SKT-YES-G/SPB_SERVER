package com.example.aegis_be.domain.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "구급대원 체크리스트 확정/수정 요청")
@Getter
@NoArgsConstructor
public class ChecklistUpdateRequest {

    @Schema(
            description = "구급대원이 확정/수정한 68개 이진 배열 체크리스트 데이터 (0 또는 1)",
            example = "[1,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0]",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "체크리스트 데이터는 필수입니다")
    @Size(min = 68, max = 68, message = "체크리스트 데이터는 68개여야 합니다")
    private List<Integer> checklistData;
}
