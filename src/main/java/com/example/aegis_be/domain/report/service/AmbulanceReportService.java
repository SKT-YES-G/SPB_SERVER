package com.example.aegis_be.domain.report.service;

import com.example.aegis_be.domain.auth.entity.FireStation;
import com.example.aegis_be.domain.auth.repository.FireStationRepository;
import com.example.aegis_be.domain.dispatch.entity.DispatchSession;
import com.example.aegis_be.domain.dispatch.repository.DispatchSessionRepository;
import com.example.aegis_be.domain.eventlog.entity.EventLog;
import com.example.aegis_be.domain.eventlog.entity.EventType;
import com.example.aegis_be.domain.eventlog.service.EventLogService;
import com.example.aegis_be.domain.report.dto.*;
import com.example.aegis_be.domain.report.entity.AmbulanceReport;
import com.example.aegis_be.domain.report.repository.AmbulanceReportRepository;
import com.example.aegis_be.global.client.AiApiClient;
import com.example.aegis_be.global.error.BusinessException;
import com.example.aegis_be.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AmbulanceReportService {

    private final AmbulanceReportRepository ambulanceReportRepository;
    private final DispatchSessionRepository dispatchSessionRepository;
    private final FireStationRepository fireStationRepository;
    private final EventLogService eventLogService;
    private final AiApiClient aiApiClient;

    @Transactional
    public AmbulanceReportResponse getReport(String name, Long sessionId) {
        DispatchSession session = findSessionByFireStation(name, sessionId);
        AmbulanceReport report = findOrCreateReport(session, sessionId);
        return AmbulanceReportResponse.from(report);
    }

    // === AI 구급일지 생성 ===

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AmbulanceReportResponse generateReport(String authorization, String name, Long sessionId) {
        DispatchSession session = findSessionByFireStation(name, sessionId);
        AmbulanceReport report = findOrCreateReport(session, sessionId);

        // EventLog에서 AI_REASONING_SAVED 이벤트의 reasoning 텍스트 추출
        List<String> reasoningList = eventLogService
                .findBySessionIdAndEventType(sessionId, EventType.AI_REASONING_SAVED)
                .stream()
                .map(EventLog::getDescription)
                .toList();

        // AI 서버 호출 (트랜잭션 밖 → DB 커넥션 점유 안 함)
        ReportGenerateResponse aiResponse = aiApiClient.requestReportGeneration(authorization, sessionId, reasoningList);

        // 명시적 save (dirty checking 대신)
        report.updateSummary(aiResponse.getSummary());
        report.updateAiChecklist(aiResponse.getAiChecklistData());
        ambulanceReportRepository.save(report);

        log.info("Report generated: reportId={}, sessionId={}, reasoningCount={}",
                report.getId(), sessionId, reasoningList.size());
        return AmbulanceReportResponse.from(report);
    }

    // === AI용 PATCH ===

    @Transactional
    public VitalsResponse updateVitals(String name, Long sessionId, VitalsUpdateRequest request) {
        DispatchSession session = findSessionByFireStation(name, sessionId);
        AmbulanceReport report = findOrCreateReport(session, sessionId);
        report.updateVitals(
                request.getSbp(), request.getDbp(), request.getRr(), request.getPr(),
                request.getTempC(), request.getSpO2(), request.getGlucose()
        );
        log.info("Vitals updated: reportId={}, sessionId={}", report.getId(), sessionId);
        return VitalsResponse.from(report);
    }

    // === 구급대원용 PATCH ===

    @Transactional
    public AmbulanceReportResponse updateChecklist(String name, Long sessionId, ChecklistUpdateRequest request) {
        DispatchSession session = findSessionByFireStation(name, sessionId);
        AmbulanceReport report = findOrCreateReport(session, sessionId);
        report.updateChecklist(request.getChecklistData());
        log.info("Checklist confirmed: reportId={}, sessionId={}", report.getId(), sessionId);
        return AmbulanceReportResponse.from(report);
    }

    @Transactional
    public AmbulanceReportResponse updateAssessment(String name, Long sessionId, AssessmentUpdateRequest request) {
        DispatchSession session = findSessionByFireStation(name, sessionId);
        AmbulanceReport report = findOrCreateReport(session, sessionId);
        report.updateAssessment(request.getChiefComplaint(), request.getAssessment(), request.getIncidentDateTime());
        log.info("Assessment updated: reportId={}, sessionId={}", report.getId(), sessionId);
        return AmbulanceReportResponse.from(report);
    }

    // === private ===

    private AmbulanceReport findOrCreateReport(DispatchSession session, Long sessionId) {
        return ambulanceReportRepository.findByDispatchSessionId(sessionId)
                .orElseGet(() -> ambulanceReportRepository.save(
                        AmbulanceReport.builder().dispatchSession(session).build()));
    }

    private DispatchSession findSessionByFireStation(String name, Long sessionId) {
        FireStation fireStation = fireStationRepository.findByName(name)
                .orElseThrow(() -> new BusinessException(ErrorCode.FIRE_STATION_NOT_FOUND));
        return dispatchSessionRepository.findByIdAndFireStationId(sessionId, fireStation.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DISPATCH_SESSION_NOT_FOUND));
    }
}
