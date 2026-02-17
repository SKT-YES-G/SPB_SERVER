package com.example.aegis_be.domain.report.repository;

import com.example.aegis_be.domain.report.entity.AmbulanceReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AmbulanceReportRepository extends JpaRepository<AmbulanceReport, Long> {

    Optional<AmbulanceReport> findByDispatchSessionId(Long dispatchSessionId);
}
