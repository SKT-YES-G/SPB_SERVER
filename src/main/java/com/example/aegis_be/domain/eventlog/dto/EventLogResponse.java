package com.example.aegis_be.domain.eventlog.dto;

import com.example.aegis_be.domain.eventlog.entity.EventLog;
import com.example.aegis_be.domain.eventlog.entity.EventType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "이벤트 로그 응답")
@Getter
@Builder
public class EventLogResponse {

    @Schema(description = "이벤트 로그 ID", example = "1")
    private Long logId;

    @Schema(description = "이벤트 유형", example = "AI_KTAS_CHANGE")
    private EventType eventType;

    @Schema(description = "이벤트 설명", example = "AI KTAS 3→2 변경")
    private String description;

    @Schema(description = "생성 시각")
    private LocalDateTime createdAt;

    public static EventLogResponse from(EventLog eventLog) {
        return EventLogResponse.builder()
                .logId(eventLog.getId())
                .eventType(eventLog.getEventType())
                .description(eventLog.getDescription())
                .createdAt(eventLog.getCreatedAt())
                .build();
    }
}
