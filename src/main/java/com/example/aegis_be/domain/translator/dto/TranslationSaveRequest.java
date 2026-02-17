package com.example.aegis_be.domain.translator.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "번역 기록 저장 요청")
@Getter
@NoArgsConstructor
public class TranslationSaveRequest {

    @Schema(description = "발화자", example = "환자", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "발화자는 필수입니다")
    private String speaker;

    @Schema(description = "원문", example = "I have chest pain", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "원문은 필수입니다")
    private String originalText;

    @Schema(description = "번역문", example = "가슴이 아파요", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "번역문은 필수입니다")
    private String translatedText;

    @Schema(description = "쉬운 번역문", example = "가슴 통증")
    private String easyTranslation;

    @Schema(description = "원문 언어", example = "영어", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "원문 언어는 필수입니다")
    private String language;
}
