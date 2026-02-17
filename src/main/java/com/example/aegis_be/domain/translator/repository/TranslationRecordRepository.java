package com.example.aegis_be.domain.translator.repository;

import com.example.aegis_be.domain.translator.entity.TranslationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TranslationRecordRepository extends JpaRepository<TranslationRecord, Long> {

    List<TranslationRecord> findByDispatchSessionIdOrderByCreatedAtAsc(Long dispatchSessionId);
}
