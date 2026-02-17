package com.example.aegis_be.domain.prektas.repository;

import com.example.aegis_be.domain.prektas.entity.PreKtas;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PreKtasRepository extends JpaRepository<PreKtas, Long> {

    Optional<PreKtas> findByDispatchSessionId(Long dispatchSessionId);
}
