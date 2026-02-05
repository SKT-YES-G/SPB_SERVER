package com.example.aegis_be.domain.auth.repository;

import com.example.aegis_be.domain.auth.entity.FireStation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FireStationRepository extends JpaRepository<FireStation, Long> {

    Optional<FireStation> findByName(String name);

    List<FireStation> findByNameContaining(String name);

    boolean existsByName(String name);
}
