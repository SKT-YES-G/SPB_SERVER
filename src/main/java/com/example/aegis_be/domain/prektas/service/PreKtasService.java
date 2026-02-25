package com.example.aegis_be.domain.prektas.service;

import com.example.aegis_be.domain.auth.entity.FireStation;
import com.example.aegis_be.domain.auth.repository.FireStationRepository;
import com.example.aegis_be.domain.dispatch.entity.DispatchSession;
import com.example.aegis_be.domain.dispatch.repository.DispatchSessionRepository;
import com.example.aegis_be.domain.eventlog.entity.EventType;
import com.example.aegis_be.domain.eventlog.service.EventLogService;
import com.example.aegis_be.domain.prektas.dto.AiKtasUpdateRequest;
import com.example.aegis_be.domain.prektas.dto.KtasSyncToggleRequest;
import com.example.aegis_be.domain.prektas.dto.ParamedicKtasUpdateRequest;
import com.example.aegis_be.domain.prektas.dto.PreKtasResponse;
import com.example.aegis_be.domain.prektas.entity.PreKtas;
import com.example.aegis_be.domain.prektas.repository.PreKtasRepository;
import com.example.aegis_be.global.error.BusinessException;
import com.example.aegis_be.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PreKtasService {

    private final PreKtasRepository preKtasRepository;
    private final DispatchSessionRepository dispatchSessionRepository;
    private final FireStationRepository fireStationRepository;
    private final EventLogService eventLogService;

    @Transactional
    public PreKtasResponse updateAiKtas(String name, Long sessionId, AiKtasUpdateRequest request) {
        DispatchSession session = findSessionByFireStation(name, sessionId);
        PreKtas preKtas = preKtasRepository.findByDispatchSessionId(sessionId)
                .orElseGet(() -> preKtasRepository.save(new PreKtas(session)));

        Integer oldLevel = preKtas.getAiKtasLevel();

        preKtas.updateAiKtas(request.getLevel(), request.getReasoning(),
                request.getStage2(), request.getStage3(), request.getStage4());

        // 등급 변경 시에만 이벤트 로그 기록
        boolean levelChanged = !Objects.equals(oldLevel, request.getLevel());
        if (levelChanged) {
            String description = oldLevel != null
                    ? "AI 중증도 판단 " + oldLevel + "→" + request.getLevel() + "등급 변경"
                    : "AI 중증도 판단 " + request.getLevel() + "등급 설정";
            eventLogService.log(session, EventType.AI_KTAS_CHANGE, description);

            if (request.getReasoning() != null) {
                eventLogService.log(session, EventType.AI_REASONING_SAVED, "판단근거: " + request.getReasoning());
            }
        }

        return PreKtasResponse.from(preKtas);
    }

    @Transactional
    public PreKtasResponse updateParamedicKtas(String name, Long sessionId, ParamedicKtasUpdateRequest request) {
        DispatchSession session = findSessionByFireStation(name, sessionId);
        PreKtas preKtas = preKtasRepository.findByDispatchSessionId(sessionId)
                .orElseGet(() -> preKtasRepository.save(new PreKtas(session)));

        Integer oldLevel = preKtas.getParamedicKtasLevel();
        boolean wasSynced = preKtas.isSynced();
        preKtas.updateParamedicKtas(request.getLevel());

        // 등급 변경 시에만 이벤트 로그 기록
        boolean levelChanged = !Objects.equals(oldLevel, request.getLevel());
        if (levelChanged) {
            String description = oldLevel != null
                    ? "사용자 중증도 " + oldLevel + "→" + request.getLevel() + "등급 변경"
                    : "사용자 중증도 " + request.getLevel() + "등급 설정";
            eventLogService.log(session, EventType.PARAMEDIC_KTAS_CHANGE, description);
        }

        if (wasSynced) {
            eventLogService.log(session, EventType.SYNC_TOGGLE, "AI 동기화 OFF");
        }

        return PreKtasResponse.from(preKtas);
    }

    @Transactional
    public PreKtasResponse toggleSync(String name, Long sessionId, KtasSyncToggleRequest request) {
        DispatchSession session = findSessionByFireStation(name, sessionId);
        PreKtas preKtas = preKtasRepository.findByDispatchSessionId(sessionId)
                .orElseGet(() -> preKtasRepository.save(new PreKtas(session)));

        preKtas.toggleSync(request.getSynced());

        String syncStatus = request.getSynced() ? "ON" : "OFF";
        eventLogService.log(session, EventType.SYNC_TOGGLE, "AI 동기화 " + syncStatus);

        return PreKtasResponse.from(preKtas);
    }

    @Transactional
    public PreKtasResponse getKtas(String name, Long sessionId) {
        DispatchSession session = findSessionByFireStation(name, sessionId);
        PreKtas preKtas = preKtasRepository.findByDispatchSessionId(sessionId)
                .orElseGet(() -> preKtasRepository.save(new PreKtas(session)));
        return PreKtasResponse.from(preKtas);
    }

    private DispatchSession findSessionByFireStation(String name, Long sessionId) {
        FireStation fireStation = fireStationRepository.findByName(name)
                .orElseThrow(() -> new BusinessException(ErrorCode.FIRE_STATION_NOT_FOUND));
        return dispatchSessionRepository.findByIdAndFireStationId(sessionId, fireStation.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DISPATCH_SESSION_NOT_FOUND));
    }
}
