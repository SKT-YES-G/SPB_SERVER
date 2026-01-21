package com.example.aegis_be.domain.healthcheck.repository;
import com.example.aegis_be.domain.healthcheck.entity.HealthCheck;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthCheckRepository extends JpaRepository<HealthCheck, Long> {
}
