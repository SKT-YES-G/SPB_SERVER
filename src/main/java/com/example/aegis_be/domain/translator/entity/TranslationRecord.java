package com.example.aegis_be.domain.translator.entity;

import com.example.aegis_be.domain.dispatch.entity.DispatchSession;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "translation_record", indexes = {
        @Index(name = "idx_translation_session", columnList = "dispatch_session_id"),
        @Index(name = "idx_translation_created", columnList = "dispatch_session_id, created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TranslationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispatch_session_id", nullable = false)
    private DispatchSession dispatchSession;

    @Column(nullable = false, length = 50)
    private String speaker;

    @Column(name = "original_text", nullable = false, columnDefinition = "TEXT")
    private String originalText;

    @Column(name = "translated_text", nullable = false, columnDefinition = "TEXT")
    private String translatedText;

    @Column(name = "easy_translation", columnDefinition = "TEXT")
    private String easyTranslation;

    @Column(nullable = false, length = 20)
    private String language;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public TranslationRecord(DispatchSession dispatchSession, String speaker,
                             String originalText, String translatedText,
                             String easyTranslation, String language) {
        this.dispatchSession = dispatchSession;
        this.speaker = speaker;
        this.originalText = originalText;
        this.translatedText = translatedText;
        this.easyTranslation = easyTranslation;
        this.language = language;
        this.createdAt = LocalDateTime.now();
    }

    public void updateEasyTranslation(String easyTranslation) {
        this.easyTranslation = easyTranslation;
    }
}
