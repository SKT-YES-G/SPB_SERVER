package com.example.aegis_be.domain.dispatch.repository;

import com.example.aegis_be.domain.dispatch.entity.DispatchSession;
import com.example.aegis_be.domain.dispatch.entity.DispatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DispatchSessionRepository extends JpaRepository<DispatchSession, Long> {

    Optional<DispatchSession> findByIdAndFireStationId(Long id, Long fireStationId);

    List<DispatchSession> findByFireStationIdAndStatusOrderByDispatchedAtDesc(Long fireStationId, DispatchStatus status);

    List<DispatchSession> findByFireStationIdOrderByDispatchedAtDesc(Long fireStationId);

    void deleteAllByFireStationId(Long fireStationId);
}
