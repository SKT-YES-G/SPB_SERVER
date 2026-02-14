package com.example.aegis_be.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 내부 오류가 발생했습니다"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C003", "허용되지 않은 HTTP 메서드입니다"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "C004", "요청한 리소스를 찾을 수 없습니다"),

    // Auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "인증이 필요합니다"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "만료된 토큰입니다"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "A004", "소방서명 또는 비밀번호가 일치하지 않습니다"),
    ALREADY_LOGGED_OUT(HttpStatus.UNAUTHORIZED, "A005", "이미 로그아웃된 사용자입니다"),

    // FireStation
    FIRE_STATION_NOT_FOUND(HttpStatus.NOT_FOUND, "F001", "소방서를 찾을 수 없습니다"),
    DUPLICATE_NAME(HttpStatus.CONFLICT, "F002", "이미 존재하는 소방서명입니다"),

    // Dispatch
    DISPATCH_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "D001", "출동 세션을 찾을 수 없습니다"),
    DISPATCH_SESSION_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "D002", "이미 종료된 출동 세션입니다"),

    // Medical
    EXTERNAL_API_ERROR(HttpStatus.BAD_GATEWAY, "M001", "외부 응급의료 API 호출에 실패했습니다"),
    API_RESPONSE_PARSE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "M002", "외부 API 응답 파싱에 실패했습니다"),
    INVALID_COORDINATES(HttpStatus.BAD_REQUEST, "M004", "유효하지 않은 좌표입니다"),
    INVALID_KTAS_LEVEL(HttpStatus.BAD_REQUEST, "M005", "KTAS 등급은 1~5 사이여야 합니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
