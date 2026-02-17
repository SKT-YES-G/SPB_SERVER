package com.example.aegis_be.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Aegis API")
                        .description("""
                                소방서 spring boot 백엔드 API

                                ## 인증 방식
                                - JWT Bearer Token 인증을 사용합니다.
                                - 로그인 API로 Access Token을 발급받은 후, 우측 상단 **Authorize** 버튼을 클릭하여 토큰을 입력하세요.
                                - 토큰 입력 시 'Bearer ' 접두사 없이 토큰 값만 입력합니다.

                                ## 토큰 유효기간
                                - Access Token: 2시간
                                - Refresh Token: 2주

                                ## 에러 응답 형식
                                ```json
                                {
                                  "success": false,
                                  "data": null,
                                  "error": {
                                    "code": "A001",
                                    "message": "에러 메시지"
                                  }
                                }
                                ```
                                """)
                        .version("v1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Access Token을 입력하세요. (Bearer 접두사 불필요)")));
    }
}