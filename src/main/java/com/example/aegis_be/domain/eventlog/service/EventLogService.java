package com.example.aegis_be.domain.eventlog.service;

import com.example.aegis_be.domain.auth.entity.FireStation;
import com.example.aegis_be.domain.auth.repository.FireStationRepository;
import com.example.aegis_be.domain.dispatch.entity.DispatchSession;
import com.example.aegis_be.domain.dispatch.repository.DispatchSessionRepository;
import com.example.aegis_be.domain.eventlog.dto.EventLogResponse;
import com.example.aegis_be.domain.eventlog.entity.EventLog;
import com.example.aegis_be.domain.eventlog.entity.EventType;
import com.example.aegis_be.domain.eventlog.repository.EventLogRepository;
import com.example.aegis_be.global.error.BusinessException;
import com.example.aegis_be.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventLogService {

    private final EventLogRepository eventLogRepository;
    private final DispatchSessionRepository dispatchSessionRepository;
    private final FireStationRepository fireStationRepository;

    public List<EventLogResponse> getLogs(String name, Long sessionId) {
        findSessionByFireStation(name, sessionId);
        return eventLogRepository.findByDispatchSessionIdOrderByCreatedAtAsc(sessionId)
                .stream()
                .map(EventLogResponse::from)
                .toList();
    }

    @Transactional
    public void log(DispatchSession session, EventType eventType, String description) {
        eventLogRepository.save(new EventLog(session, eventType, description));
    }

    public List<EventLog> findBySessionIdAndEventType(Long sessionId, EventType eventType) {
        return eventLogRepository.findByDispatchSessionIdAndEventTypeOrderByCreatedAtAsc(sessionId, eventType);
    }

    private DispatchSession findSessionByFireStation(String name, Long sessionId) {
        FireStation fireStation = fireStationRepository.findByName(name)
                .orElseThrow(() -> new BusinessException(ErrorCode.FIRE_STATION_NOT_FOUND));
        return dispatchSessionRepository.findByIdAndFireStationId(sessionId, fireStation.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DISPATCH_SESSION_NOT_FOUND));
    }
}
