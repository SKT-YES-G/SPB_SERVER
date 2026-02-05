package com.example.aegis_be.domain.auth.dto;

import com.example.aegis_be.domain.auth.entity.FireStation;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FireStationSearchResponse {

    private Long id;
    private String name;

    public static FireStationSearchResponse from(FireStation fireStation) {
        return FireStationSearchResponse.builder()
                .id(fireStation.getId())
                .name(fireStation.getName())
                .build();
    }
}
