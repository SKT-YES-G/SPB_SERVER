package com.example.aegis_be.domain.translator.dto;

import com.example.aegis_be.domain.translator.entity.TranslationRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "번역 기록 응답")
@Getter
@Builder
public class TranslationResponse {

    @Schema(description = "번역 기록 ID", example = "1")
    private Long translationId;

    @Schema(description = "발화자", example = "환자")
    private String speaker;

    @Schema(description = "원문", example = "I have chest pain")
    private String originalText;

    @Schema(description = "번역문", example = "가슴이 아파요")
    private String translatedText;

    @Schema(description = "쉬운 번역문", example = "가슴 통증")
    private String easyTranslation;

    @Schema(description = "원문 언어", example = "영어")
    private String language;

    @Schema(description = "생성 시각")
    private LocalDateTime createdAt;

    public static TranslationResponse from(TranslationRecord record) {
        return TranslationResponse.builder()
                .translationId(record.getId())
                .speaker(record.getSpeaker())
                .originalText(record.getOriginalText())
                .translatedText(record.getTranslatedText())
                .easyTranslation(record.getEasyTranslation())
                .language(record.getLanguage())
                .createdAt(record.getCreatedAt())
                .build();
    }
}
