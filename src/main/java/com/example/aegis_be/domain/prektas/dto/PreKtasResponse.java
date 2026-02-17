package com.example.aegis_be.domain.prektas.dto;

import com.example.aegis_be.domain.prektas.entity.PreKtas;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "KTAS 정보 응답")
@Getter
@Builder
public class PreKtasResponse {

    @Schema(description = "세션 ID", example = "1")
    private Long sessionId;

    @Schema(description = "AI 판정 KTAS 등급", example = "3")
    private Integer aiKtasLevel;

    @Schema(description = "AI 판정 근거", example = "환자 호흡곤란 및 의식 저하로 KTAS 3 판정")
    private String aiReasoning;

    @Schema(description = "구급대원 판정 KTAS 등급", example = "2")
    private Integer paramedicKtasLevel;

    @Schema(description = "동기화 상태 (true: AI KTAS 연동, false: 수동)", example = "true")
    private boolean synced;

    @Schema(description = "생성 시각")
    private LocalDateTime createdAt;

    @Schema(description = "수정 시각")
    private LocalDateTime updatedAt;

    public static PreKtasResponse from(PreKtas preKtas) {
        return PreKtasResponse.builder()
                .sessionId(preKtas.getDispatchSession().getId())
                .aiKtasLevel(preKtas.getAiKtasLevel())
                .aiReasoning(preKtas.getAiReasoning())
                .paramedicKtasLevel(preKtas.getParamedicKtasLevel())
                .synced(preKtas.isSynced())
                .createdAt(preKtas.getCreatedAt())
                .updatedAt(preKtas.getUpdatedAt())
                .build();
    }
}
