package com.example.aegis_be.global.common;

import com.example.aegis_be.global.error.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ErrorInfo error;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    public static ApiResponse<Void> success() {
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }

    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return ApiResponse.<Void>builder()
                .success(false)
                .error(ErrorInfo.of(errorCode))
                .build();
    }

    public static ApiResponse<Void> error(ErrorCode errorCode, String message) {
        return ApiResponse.<Void>builder()
                .success(false)
                .error(ErrorInfo.of(errorCode, message))
                .build();
    }

    @Getter
    @Builder
    public static class ErrorInfo {
        private final String code;
        private final String message;

        public static ErrorInfo of(ErrorCode errorCode) {
            return ErrorInfo.builder()
                    .code(errorCode.getCode())
                    .message(errorCode.getMessage())
                    .build();
        }

        public static ErrorInfo of(ErrorCode errorCode, String message) {
            return ErrorInfo.builder()
                    .code(errorCode.getCode())
                    .message(message)
                    .build();
        }
    }
}
