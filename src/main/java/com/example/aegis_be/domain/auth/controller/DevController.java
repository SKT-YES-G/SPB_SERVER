package com.example.aegis_be.domain.auth.controller;

import com.example.aegis_be.domain.auth.dto.FireStationCreateRequest;
import com.example.aegis_be.domain.auth.entity.FireStation;
import com.example.aegis_be.domain.auth.repository.FireStationRepository;
import com.example.aegis_be.global.common.ApiResponse;
import com.example.aegis_be.global.error.BusinessException;
import com.example.aegis_be.global.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Dev", description = "개발용 API")
@Profile("!prod")
@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
public class DevController {

    private final FireStationRepository fireStationRepository;
    private final PasswordEncoder passwordEncoder;

    @Operation(summary = "소방서 계정 생성", description = "테스트용 소방서 계정 생성")
    @PostMapping("/fire-stations")
    public ApiResponse<Void> createFireStation(@Valid @RequestBody FireStationCreateRequest request) {
        if (fireStationRepository.existsByName(request.getName())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NAME);
        }

        fireStationRepository.save(FireStation.builder()
                .name(request.getName())
                .password(passwordEncoder.encode(request.getPassword()))
                .build());

        return ApiResponse.success();
    }

    @Operation(summary = "전체 소방서 목록 조회", description = "등록된 모든 소방서 목록 확인")
    @GetMapping("/fire-stations")
    public ApiResponse<List<FireStationListItem>> listFireStations() {
        List<FireStationListItem> list = fireStationRepository.findAll().stream()
                .map(fs -> new FireStationListItem(fs.getId(), fs.getName()))
                .toList();
        return ApiResponse.success(list);
    }

    @Operation(summary = "소방서 계정 삭제", description = "테스트 계정 삭제")
    @DeleteMapping("/fire-stations/{id}")
    public ApiResponse<Void> deleteFireStation(@PathVariable Long id) {
        if (!fireStationRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.FIRE_STATION_NOT_FOUND);
        }
        fireStationRepository.deleteById(id);
        return ApiResponse.success();
    }

    public record FireStationListItem(Long id, String name) {}
}