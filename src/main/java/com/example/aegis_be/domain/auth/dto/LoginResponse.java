package com.example.aegis_be.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private FireStationInfo fireStation;

    @Getter
    @Builder
    public static class FireStationInfo {
        private String name;
    }
}
