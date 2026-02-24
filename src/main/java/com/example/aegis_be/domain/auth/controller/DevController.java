package com.example.aegis_be.domain.auth.controller;

import com.example.aegis_be.domain.auth.dto.FireStationCreateRequest;
import com.example.aegis_be.domain.auth.entity.FireStation;
import com.example.aegis_be.domain.auth.repository.FireStationRepository;
import com.example.aegis_be.domain.dispatch.repository.DispatchSessionRepository;
import com.example.aegis_be.domain.prektas.repository.PreKtasRepository;
import com.example.aegis_be.global.common.ApiResponse;
import com.example.aegis_be.global.error.BusinessException;
import com.example.aegis_be.global.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Dev", description = "개발용 API")
@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
public class DevController {

    private final FireStationRepository fireStationRepository;
    private final DispatchSessionRepository dispatchSessionRepository;
    private final PreKtasRepository preKtasRepository;
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

    @Operation(summary = "소방서 계정 삭제", description = "테스트 계정 삭제 (연관된 출동 세션도 함께 삭제)")
    @DeleteMapping("/fire-stations/{id}")
    @Transactional
    public ApiResponse<Void> deleteFireStation(@PathVariable Long id) {
        if (!fireStationRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.FIRE_STATION_NOT_FOUND);
        }
        dispatchSessionRepository.deleteAllByFireStationId(id);
        fireStationRepository.deleteById(id);
        return ApiResponse.success();
    }

    @Operation(summary = "환자 AI 추천 진료과 조회", description = "세션 ID로 AI 추천 진료과 목록 조회 (테스트용)")
    @GetMapping("/sessions/{sessionId}/departments")
    public ApiResponse<List<String>> getSessionDepartments(@PathVariable Long sessionId) {
        List<String> departments = preKtasRepository.findByDispatchSessionId(sessionId)
                .map(preKtas -> preKtas.getAiDepartments() != null ? preKtas.getAiDepartments() : List.<String>of())
                .orElse(List.of());
        return ApiResponse.success(departments);
    }

    public record FireStationListItem(Long id, String name) {}
}
