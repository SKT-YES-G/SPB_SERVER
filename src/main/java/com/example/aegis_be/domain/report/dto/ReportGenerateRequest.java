package com.example.aegis_be.domain.report.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ReportGenerateRequest {

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("prektas_rationale")
    private List<String> prektasRationale;
}
