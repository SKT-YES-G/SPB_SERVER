package com.example.aegis_be.domain.healthcheck.controller;

import com.example.aegis_be.domain.healthcheck.service.HealthCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Health Check", description = "시스템 상태 확인 API - 서버 및 인프라 연결 상태를 확인합니다.")
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class HealthCheckController {
	private final HealthCheckService service;

	@Operation(
			summary = "DB 연결 테스트",
			description = """
					MySQL 데이터베이스 연결 상태를 확인합니다.

					**응답**: 연결 성공 시 "DB connection successful" 반환

					**사용 목적**:
					- 서버 배포 후 DB 연결 확인
					- 장애 발생 시 DB 연결 상태 진단

					**인증**: 불필요 (공개 API)
					"""
	)
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "200",
					description = "DB 연결 성공"
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "500",
					description = "DB 연결 실패"
			)
	})
	@GetMapping("/db")
	public String testDB() {
		return service.testDB();
	}
}
