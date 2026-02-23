package com.example.aegis_be.domain.eventlog.repository;

import com.example.aegis_be.domain.eventlog.entity.EventLog;
import com.example.aegis_be.domain.eventlog.entity.EventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventLogRepository extends JpaRepository<EventLog, Long> {

    List<EventLog> findByDispatchSessionIdOrderByCreatedAtAsc(Long dispatchSessionId);

    List<EventLog> findByDispatchSessionIdAndEventTypeOrderByCreatedAtAsc(Long dispatchSessionId, EventType eventType);
}
