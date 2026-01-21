package com.example.aegis_be.domain.healthcheck.service;

import com.example.aegis_be.domain.healthcheck.entity.HealthCheck;
import com.example.aegis_be.domain.healthcheck.repository.HealthCheckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HealthCheckService {
	private final HealthCheckRepository repository;

	@Transactional
	public String testDB() {
		repository.save(new HealthCheck("OK"));
		return "DB Test Success! Total: " + repository.count();
	}
}
