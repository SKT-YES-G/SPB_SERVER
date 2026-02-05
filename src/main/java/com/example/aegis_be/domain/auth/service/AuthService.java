package com.example.aegis_be.domain.auth.service;

import com.example.aegis_be.domain.auth.dto.*;
import com.example.aegis_be.domain.auth.entity.FireStation;
import com.example.aegis_be.domain.auth.repository.FireStationRepository;
import com.example.aegis_be.global.error.BusinessException;
import com.example.aegis_be.global.error.ErrorCode;
import com.example.aegis_be.global.security.jwt.JwtTokenProvider;
import com.example.aegis_be.global.security.jwt.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final FireStationRepository fireStationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;

    public LoginResponse login(LoginRequest request) {
        FireStation fireStation = fireStationRepository
                .findByName(request.getName())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), fireStation.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken = jwtTokenProvider.createAccessToken(fireStation.getName());
        String refreshToken = jwtTokenProvider.createRefreshToken(fireStation.getName());

        tokenService.saveRefreshToken(fireStation.getName(), refreshToken);

        log.info("Login successful: {}", fireStation.getName());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .fireStation(LoginResponse.FireStationInfo.builder()
                        .name(fireStation.getName())
                        .build())
                .build();
    }

    public TokenRefreshResponse refresh(TokenRefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        String name = jwtTokenProvider.getSubject(refreshToken);

        if (!tokenService.validateRefreshToken(name, refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        String newAccessToken = jwtTokenProvider.createAccessToken(name);

        return TokenRefreshResponse.builder()
                .accessToken(newAccessToken)
                .build();
    }

    public void logout(String name) {
        tokenService.deleteRefreshToken(name);
        log.info("Logout successful: {}", name);
    }

    public List<FireStationSearchResponse> searchFireStations(String query) {
        return fireStationRepository.findByNameContaining(query).stream()
                .map(FireStationSearchResponse::from)
                .toList();
    }
}
