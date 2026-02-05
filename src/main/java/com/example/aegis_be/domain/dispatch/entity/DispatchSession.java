package com.example.aegis_be.domain.dispatch.entity;

import com.example.aegis_be.domain.auth.entity.FireStation;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "dispatch_session",
        indexes = {
                @Index(name = "idx_dispatch_fire_station", columnList = "fire_station_id"),
                @Index(name = "idx_dispatch_status", columnList = "status")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DispatchSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fire_station_id", nullable = false)
    private FireStation fireStation;

    @Column(name = "representative_name", nullable = false, length = 30)
    private String representativeName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DispatchStatus status;

    @Column(name = "dispatched_at", nullable = false)
    private LocalDateTime dispatchedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Builder
    public DispatchSession(FireStation fireStation, String representativeName) {
        this.fireStation = fireStation;
        this.representativeName = representativeName;
        this.status = DispatchStatus.ACTIVE;
        this.dispatchedAt = LocalDateTime.now();
    }

    public void complete() {
        this.status = DispatchStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return this.status == DispatchStatus.ACTIVE;
    }
}
