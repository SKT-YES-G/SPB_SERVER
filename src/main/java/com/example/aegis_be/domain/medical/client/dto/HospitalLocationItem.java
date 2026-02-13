package com.example.aegis_be.domain.medical.client.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HospitalLocationItem {

    private String hpid;
    private String dutyName;
    private String dutyAddr;
    private String dutyTel1;
    private String dutyTel3;
    private String dgidIdName;
    private double wgs84Lat;
    private double wgs84Lon;
}
