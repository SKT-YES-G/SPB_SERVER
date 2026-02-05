package com.example.aegis_be.domain.dispatch.service;

import com.example.aegis_be.domain.auth.entity.FireStation;
import com.example.aegis_be.domain.auth.repository.FireStationRepository;
import com.example.aegis_be.domain.dispatch.dto.DispatchSessionCreateRequest;
import com.example.aegis_be.domain.dispatch.dto.DispatchSessionResponse;
import com.example.aegis_be.domain.dispatch.entity.DispatchSession;
import com.example.aegis_be.domain.dispatch.entity.DispatchStatus;
import com.example.aegis_be.domain.dispatch.repository.DispatchSessionRepository;
import com.example.aegis_be.global.error.BusinessException;
import com.example.aegis_be.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DispatchService {

    private final DispatchSessionRepository dispatchSessionRepository;
    private final FireStationRepository fireStationRepository;

    @Transactional
    public DispatchSessionResponse createSession(String name, DispatchSessionCreateRequest request) {
        FireStation fireStation = fireStationRepository.findByName(name)
                .orElseThrow(() -> new BusinessException(ErrorCode.FIRE_STATION_NOT_FOUND));

        DispatchSession session = DispatchSession.builder()
                .fireStation(fireStation)
                .representativeName(request.getRepresentativeName())
                .build();

        dispatchSessionRepository.save(session);

        log.info("Dispatch session created: sessionId={}, representative={}", session.getId(), request.getRepresentativeName());

        return DispatchSessionResponse.from(session);
    }

    public DispatchSessionResponse getSession(String name, Long sessionId) {
        FireStation fireStation = findFireStation(name);
        DispatchSession session = findSessionByFireStation(sessionId, fireStation.getId());
        return DispatchSessionResponse.from(session);
    }

    public List<DispatchSessionResponse> getActiveSessions(String name) {
        FireStation fireStation = findFireStation(name);
        return dispatchSessionRepository
                .findByFireStationIdAndStatusOrderByDispatchedAtDesc(fireStation.getId(), DispatchStatus.ACTIVE)
                .stream()
                .map(DispatchSessionResponse::from)
                .toList();
    }

    public List<DispatchSessionResponse> getAllSessions(String name) {
        FireStation fireStation = findFireStation(name);
        return dispatchSessionRepository
                .findByFireStationIdOrderByDispatchedAtDesc(fireStation.getId())
                .stream()
                .map(DispatchSessionResponse::from)
                .toList();
    }

    @Transactional
    public DispatchSessionResponse completeSession(String name, Long sessionId) {
        FireStation fireStation = findFireStation(name);
        DispatchSession session = findSessionByFireStation(sessionId, fireStation.getId());

        if (!session.isActive()) {
            throw new BusinessException(ErrorCode.DISPATCH_SESSION_NOT_ACTIVE);
        }

        session.complete();

        log.info("Dispatch session completed: sessionId={}", sessionId);

        return DispatchSessionResponse.from(session);
    }

    private DispatchSession findSessionByFireStation(Long sessionId, Long fireStationId) {
        return dispatchSessionRepository.findByIdAndFireStationId(sessionId, fireStationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DISPATCH_SESSION_NOT_FOUND));
    }

    private FireStation findFireStation(String name) {
        return fireStationRepository.findByName(name)
                .orElseThrow(() -> new BusinessException(ErrorCode.FIRE_STATION_NOT_FOUND));
    }
}
