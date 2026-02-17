package com.example.aegis_be.domain.prektas.entity;

import com.example.aegis_be.domain.dispatch.entity.DispatchSession;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "pre_ktas", indexes = {
        @Index(name = "idx_pre_ktas_session", columnList = "dispatch_session_id", unique = true)
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PreKtas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispatch_session_id", nullable = false, unique = true)
    private DispatchSession dispatchSession;

    @Column(name = "ai_ktas_level")
    private Integer aiKtasLevel;

    @Column(name = "ai_reasoning", columnDefinition = "TEXT")
    private String aiReasoning;

    @Column(name = "paramedic_ktas_level")
    private Integer paramedicKtasLevel;

    @Column(nullable = false)
    private boolean synced = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public PreKtas(DispatchSession dispatchSession) {
        this.dispatchSession = dispatchSession;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void updateAiKtas(Integer level, String reasoning) {
        this.aiKtasLevel = level;
        this.aiReasoning = reasoning;
        if (this.synced) {
            this.paramedicKtasLevel = level;
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void updateParamedicKtas(Integer level) {
        this.paramedicKtasLevel = level;
        this.synced = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void toggleSync(boolean synced) {
        this.synced = synced;
        if (synced && this.aiKtasLevel != null) {
            this.paramedicKtasLevel = this.aiKtasLevel;
        }
        this.updatedAt = LocalDateTime.now();
    }
}
