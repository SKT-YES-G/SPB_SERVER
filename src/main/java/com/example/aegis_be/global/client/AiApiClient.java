package com.example.aegis_be.global.client;

import com.example.aegis_be.domain.report.dto.ReportGenerateRequest;
import com.example.aegis_be.domain.report.dto.ReportGenerateResponse;
import com.example.aegis_be.global.error.BusinessException;
import com.example.aegis_be.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiApiClient {

    private final RestClient restClient;
    private final AiServerProperties aiServerProperties;

    public String requestEasyTranslation(String translatedText) {
        String url = aiServerProperties.getBaseUrl() + aiServerProperties.getEasyTranslationPath();
        try {
            Map<String, String> response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", translatedText))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null || !response.containsKey("simplified_text")) {
                throw new BusinessException(ErrorCode.AI_SERVER_ERROR);
            }
            return response.get("simplified_text");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI easy translation request failed: url={}, error={}", url, e.getMessage());
            throw new BusinessException(ErrorCode.AI_SERVER_ERROR);
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> requestDepartmentRecommendation(String text) {
        String url = aiServerProperties.getBaseUrl() + aiServerProperties.getDepartmentPickPath();
        try {
            Map<String, Object> response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", text))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null || !response.containsKey("departments")) {
                throw new BusinessException(ErrorCode.AI_SERVER_ERROR);
            }
            return (List<String>) response.get("departments");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI department recommendation request failed: url={}, error={}", url, e.getMessage());
            throw new BusinessException(ErrorCode.AI_SERVER_ERROR);
        }
    }

    public ReportGenerateResponse requestReportGeneration(String authorization, Long sessionId, List<String> reasoningList) {
        String url = aiServerProperties.getBaseUrl() + aiServerProperties.getReportGenerationPath();
        try {
            ReportGenerateResponse response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", authorization)
                    .body(new ReportGenerateRequest(String.valueOf(sessionId), reasoningList))
                    .retrieve()
                    .body(ReportGenerateResponse.class);

            if (response == null) {
                throw new BusinessException(ErrorCode.AI_SERVER_ERROR);
            }
            return response;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI report generation request failed: url={}, error={}", url, e.getMessage());
            throw new BusinessException(ErrorCode.AI_SERVER_ERROR);
        }
    }
}
