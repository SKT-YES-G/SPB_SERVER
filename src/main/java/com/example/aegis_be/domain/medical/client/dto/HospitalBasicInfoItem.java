package com.example.aegis_be.domain.medical.client.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HospitalBasicInfoItem {

    private String hpid;
    private String dutyName;
    private String dutyAddr;
    private String dutyTel1;
    private String dutyTel3;
    private String dgidIdName;
    private String dutyEryn;
    private String dutyHayn;
    private String dutyHano;
    private String dutyInf;
    private String dutyMapimg;
    private String dutyTime1s;
    private String dutyTime1c;
    private String dutyTime2s;
    private String dutyTime2c;
    private String dutyTime3s;
    private String dutyTime3c;
    private String dutyTime4s;
    private String dutyTime4c;
    private String dutyTime5s;
    private String dutyTime5c;
    private String dutyTime6s;
    private String dutyTime6c;
    private String dutyTime7s;
    private String dutyTime7c;
    private String dutyTime8s;
    private String dutyTime8c;
    private double wgs84Lat;
    private double wgs84Lon;
    private int hpbdn;
    private int hpccuyn;
    private int hpcuyn;
    private int hperyn;
    private int hpgryn;
    private int hpicuyn;
    private int hpnicuyn;
    private int hpopyn;
}
