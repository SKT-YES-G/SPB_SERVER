package com.example.aegis_be.domain.dispatch.dto;

import com.example.aegis_be.domain.dispatch.entity.DispatchSession;
import com.example.aegis_be.domain.dispatch.entity.DispatchStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DispatchSessionResponse {

    private Long sessionId;
    private String fireStationName;
    private String representativeName;
    private DispatchStatus status;
    private LocalDateTime dispatchedAt;
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
