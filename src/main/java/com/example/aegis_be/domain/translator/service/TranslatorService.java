package com.example.aegis_be.domain.translator.service;

import com.example.aegis_be.domain.auth.entity.FireStation;
import com.example.aegis_be.domain.auth.repository.FireStationRepository;
import com.example.aegis_be.domain.dispatch.entity.DispatchSession;
import com.example.aegis_be.domain.dispatch.repository.DispatchSessionRepository;
import com.example.aegis_be.domain.translator.dto.TranslationResponse;
import com.example.aegis_be.domain.translator.dto.TranslationSaveRequest;
import com.example.aegis_be.domain.translator.entity.TranslationRecord;
import com.example.aegis_be.domain.translator.repository.TranslationRecordRepository;
import com.example.aegis_be.global.client.AiApiClient;
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
public class TranslatorService {

    private final TranslationRecordRepository translationRecordRepository;
    private final DispatchSessionRepository dispatchSessionRepository;
    private final FireStationRepository fireStationRepository;
    private final AiApiClient aiApiClient;

    @Transactional
    public TranslationResponse saveTranslation(String name, Long sessionId, TranslationSaveRequest request) {
        DispatchSession session = findSessionByFireStation(name, sessionId);

        TranslationRecord record = TranslationRecord.builder()
                .dispatchSession(session)
                .speaker(request.getSpeaker())
                .originalText(request.getOriginalText())
                .translatedText(request.getTranslatedText())
                .easyTranslation(request.getEasyTranslation())
                .language(request.getLanguage())
                .build();

        translationRecordRepository.save(record);

        return TranslationResponse.from(record);
    }

    public List<TranslationResponse> getTranslations(String name, Long sessionId) {
        findSessionByFireStation(name, sessionId);
        return translationRecordRepository.findByDispatchSessionIdOrderByCreatedAtAsc(sessionId)
                .stream()
                .map(TranslationResponse::from)
                .toList();
    }

    @Transactional
    public TranslationResponse generateEasyTranslation(String name, Long sessionId, Long translationId) {
        findSessionByFireStation(name, sessionId);

        TranslationRecord record = translationRecordRepository.findById(translationId)
                .filter(r -> r.getDispatchSession().getId().equals(sessionId))
                .orElseThrow(() -> new BusinessException(ErrorCode.TRANSLATION_RECORD_NOT_FOUND));

        String easyTranslation = aiApiClient.requestEasyTranslation(record.getTranslatedText());

        record.updateEasyTranslation(easyTranslation);
        log.info("Easy translation generated: translationId={}, sessionId={}", translationId, sessionId);

        return TranslationResponse.from(record);
    }

    private DispatchSession findSessionByFireStation(String name, Long sessionId) {
        FireStation fireStation = fireStationRepository.findByName(name)
                .orElseThrow(() -> new BusinessException(ErrorCode.FIRE_STATION_NOT_FOUND));
        return dispatchSessionRepository.findByIdAndFireStationId(sessionId, fireStation.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DISPATCH_SESSION_NOT_FOUND));
    }
}
