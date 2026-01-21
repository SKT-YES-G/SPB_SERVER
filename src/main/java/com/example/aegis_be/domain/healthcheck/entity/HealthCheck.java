package com.example.aegis_be.domain.healthcheck.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class HealthCheck {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String status;
	private LocalDateTime checkedAt;

	public HealthCheck(String status) {
		this.status = status;
		this.checkedAt = LocalDateTime.now();
	}
}
