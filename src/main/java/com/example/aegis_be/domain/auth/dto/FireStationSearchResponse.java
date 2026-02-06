package com.example.aegis_be.domain.auth.dto;

import com.example.aegis_be.domain.auth.entity.FireStation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "소방서 검색 결과")
@Getter
@Builder
public class FireStationSearchResponse {

    @Schema(description = "소방서 고유 ID", example = "1")
    private Long id;

    @Schema(description = "소방서명", example = "마포소방서")
    private String name;

    public static FireStationSearchResponse from(FireStation fireStation) {
        return FireStationSearchResponse.builder()
                .id(fireStation.getId())
                .name(fireStation.getName())
                .build();
    }
}
