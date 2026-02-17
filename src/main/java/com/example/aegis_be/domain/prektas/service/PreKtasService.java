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
        preKtas.updateAiKtas(request.getLevel(), request.getReasoning());

        String description = oldLevel != null
                ? "AI KTAS " + oldLevel + "→" + request.getLevel() + " 변경"
                : "AI KTAS " + request.getLevel() + " 설정";
        eventLogService.log(session, EventType.AI_KTAS_CHANGE, description);

        if (preKtas.isSynced()) {
            eventLogService.log(session, EventType.PARAMEDIC_KTAS_CHANGE,
                    "구급대원 KTAS " + request.getLevel() + " 동기화 (AI 연동)");
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

        String description = oldLevel != null
                ? "구급대원 KTAS " + oldLevel + "→" + request.getLevel() + " 변경"
                : "구급대원 KTAS " + request.getLevel() + " 설정";
        eventLogService.log(session, EventType.PARAMEDIC_KTAS_CHANGE, description);

        if (wasSynced) {
            eventLogService.log(session, EventType.SYNC_TOGGLE, "KTAS 동기화 OFF (수동 변경)");
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
        eventLogService.log(session, EventType.SYNC_TOGGLE, "KTAS 동기화 " + syncStatus);

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
