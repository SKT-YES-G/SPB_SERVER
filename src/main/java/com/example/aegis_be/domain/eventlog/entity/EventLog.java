package com.example.aegis_be.domain.eventlog.entity;

import com.example.aegis_be.domain.dispatch.entity.DispatchSession;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_log", indexes = {
        @Index(name = "idx_event_log_session", columnList = "dispatch_session_id"),
        @Index(name = "idx_event_log_created", columnList = "dispatch_session_id, created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispatch_session_id", nullable = false)
    private DispatchSession dispatchSession;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private EventType eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public EventLog(DispatchSession dispatchSession, EventType eventType, String description) {
        this.dispatchSession = dispatchSession;
        this.eventType = eventType;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }
}
