package com.example.aegis_be.domain.healthcheck.controller;

import com.example.aegis_be.domain.healthcheck.service.HealthCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class HealthCheckController {
	private final HealthCheckService service;

	@GetMapping("/db")
	public String testDB() {
		return service.testDB();
	}
}
