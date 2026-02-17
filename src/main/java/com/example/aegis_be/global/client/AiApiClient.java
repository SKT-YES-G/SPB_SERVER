package com.example.aegis_be.global.client;

import com.example.aegis_be.global.error.BusinessException;
import com.example.aegis_be.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiApiClient {

    private final RestClient restClient;
    private final AiServerProperties aiServerProperties;

    public String requestEasyTranslation(String translatedText, String language) {
        String url = aiServerProperties.getBaseUrl() + aiServerProperties.getEasyTranslationPath();
        try {
            Map<String, String> response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", translatedText, "language", language))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null || !response.containsKey("easyTranslation")) {
                throw new BusinessException(ErrorCode.AI_SERVER_ERROR);
            }
            return response.get("easyTranslation");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI easy translation request failed: url={}, error={}", url, e.getMessage());
            throw new BusinessException(ErrorCode.AI_SERVER_ERROR);
        }
    }
}
