package com.example.aegis_be.domain.medical.client.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BedAvailabilityItem {

    private String hpid;
    private String dutyName;
    private int hvec;
    private int hvoc;
    private int hvcc;
    private int hvncc;
    private int hvicc;
    private int hvgc;
    private String hvamyn;
}
