package com.example.aegis_be.global.client;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ai-server")
public class AiServerProperties {

    private String baseUrl;
    private String easyTranslationPath;
}
