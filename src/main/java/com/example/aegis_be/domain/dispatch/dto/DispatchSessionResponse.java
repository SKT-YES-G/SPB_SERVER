package com.example.aegis_be.domain.dispatch.dto;

import com.example.aegis_be.domain.dispatch.entity.DispatchSession;
import com.example.aegis_be.domain.dispatch.entity.DispatchStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "출동 세션 응답")
@Getter
@Builder
public class DispatchSessionResponse {

    @Schema(description = "세션 고유 ID", example = "1")
    private Long sessionId;

    @Schema(description = "소방서명", example = "마포소방서")
    private String fireStationName;

    @Schema(description = "출동 대표자명", example = "김응급")
    private String representativeName;

    @Schema(
            description = "세션 상태 (ACTIVE: 출동 중, COMPLETED: 완료)",
            example = "ACTIVE"
    )
    private DispatchStatus status;

    @Schema(
            description = "출동 시각 (KST)",
            example = "2024-01-15T14:30:00"
    )
    private LocalDateTime dispatchedAt;

    @Schema(
            description = "완료 시각 (KST) - 완료 전에는 null",
            example = "2024-01-15T16:45:00",
            nullable = true
    )
    private LocalDateTime completedAt;

    public static DispatchSessionResponse from(DispatchSession session) {
        return DispatchSessionResponse.builder()
                .sessionId(session.getId())
                .fireStationName(session.getFireStation().getName())
                .representativeName(session.getRepresentativeName())
                .status(session.getStatus())
                .dispatchedAt(session.getDispatchedAt())
                .completedAt(session.getCompletedAt())
                .build();
    }
}
