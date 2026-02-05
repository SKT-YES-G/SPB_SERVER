package com.example.aegis_be.domain.dispatch.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DispatchStatus {

    ACTIVE("출동 중"),
    COMPLETED("출동 완료");

    private final String description;
}
