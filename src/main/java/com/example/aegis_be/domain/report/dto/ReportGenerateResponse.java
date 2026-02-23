package com.example.aegis_be.domain.report.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
public class ReportGenerateResponse {

    @JsonProperty("session_id")
    private String sessionId;

    private String message;

    @JsonProperty("ai_checklist_data")
    private List<Integer> aiChecklistData;

    private Map<String, Object> selection;

    private String summary;
}
