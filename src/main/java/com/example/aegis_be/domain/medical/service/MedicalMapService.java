package com.example.aegis_be.domain.medical.service;

import com.example.aegis_be.domain.medical.client.EmergencyApiClient;
import com.example.aegis_be.domain.medical.client.dto.BedAvailabilityItem;
import com.example.aegis_be.domain.medical.client.dto.HospitalBasicInfoItem;
import com.example.aegis_be.domain.medical.client.dto.HospitalLocationItem;
import com.example.aegis_be.domain.medical.dto.HospitalDetailResponse;
import com.example.aegis_be.domain.medical.dto.HospitalRankItem;
import com.example.aegis_be.domain.medical.dto.HospitalSearchRequest;
import com.example.aegis_be.domain.medical.dto.HospitalSearchResponse;
import com.example.aegis_be.global.error.BusinessException;
import com.example.aegis_be.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MedicalMapService {

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double DISTANCE_DECAY_RATE = 0.1;
    private static final double MAX_BED_COUNT = 20.0;
    private static final int MAX_RESULTS = 15;

    private static final double WEIGHT_DISTANCE_WITH_KTAS = 0.3;
    private static final double WEIGHT_BED_WITH_KTAS = 0.2;
    private static final double WEIGHT_KTAS = 0.5;

    private static final double WEIGHT_DISTANCE_WITHOUT_KTAS = 0.6;
    private static final double WEIGHT_BED_WITHOUT_KTAS = 0.4;

    private static final double[][] REGION_CENTERS = {
            // { lat, lon, index }
            {37.5665, 126.978},   // 0  서울특별시
            {35.1796, 129.0756},  // 1  부산광역시
            {35.8714, 128.6014},  // 2  대구광역시
            {37.4563, 126.7052},  // 3  인천광역시
            {35.1595, 126.8526},  // 4  광주광역시
            {36.3504, 127.3845},  // 5  대전광역시
            {35.5384, 129.3114},  // 6  울산광역시
            {36.4800, 127.0000},  // 7  세종특별자치시
            {37.2750, 127.0095},  // 8  경기도
            {37.8228, 128.1555},  // 9  강원특별자치도
            {36.6357, 127.4917},  // 10 충청북도
            {36.6588, 126.6728},  // 11 충청남도
            {35.8202, 127.1089},  // 12 전북특별자치도
            {34.8161, 126.4629},  // 13 전라남도
            {36.4919, 128.8889},  // 14 경상북도
            {35.4606, 128.2132},  // 15 경상남도
            {33.4890, 126.4983},  // 16 제주특별자치도
    };

    private static final String[] REGION_NAMES = {
            "서울특별시", "부산광역시", "대구광역시", "인천광역시",
            "광주광역시", "대전광역시", "울산광역시", "세종특별자치시",
            "경기도", "강원특별자치도", "충청북도", "충청남도",
            "전북특별자치도", "전라남도", "경상북도", "경상남도", "제주특별자치도"
    };

    private final EmergencyApiClient emergencyApiClient;

    public HospitalSearchResponse searchHospitals(HospitalSearchRequest request) {
        String stage1 = resolveRegion(request.getLatitude(), request.getLongitude());
        log.info("좌표({}, {}) → 시도: {}", request.getLatitude(), request.getLongitude(), stage1);

        List<HospitalLocationItem> hospitals = emergencyApiClient.getHospitalList(stage1, null);
        Map<String, BedAvailabilityItem> bedMap = fetchBedAvailability(stage1, null);

        boolean ktasApplied = request.getKtasLevel() != null;

        List<HospitalRankItem> sorted = hospitals.stream()
                .map(loc -> {
                    double distance = calculateHaversineDistance(
                            request.getLatitude(), request.getLongitude(),
                            loc.getWgs84Lat(), loc.getWgs84Lon());

                    BedAvailabilityItem bed = bedMap.get(loc.getHpid());
                    int availableBeds = (bed != null) ? bed.getHvec() : 0;

                    double score = calculateScore(distance, availableBeds, loc.getDgidIdName(),
                            request.getKtasLevel());

                    return HospitalRankItem.builder()
                            .score(Math.round(score * 100.0) / 100.0)
                            .hpid(loc.getHpid())
                            .hospitalName(loc.getDutyName())
                            .address(loc.getDutyAddr())
                            .tel(loc.getDutyTel1())
                            .emergencyTel(loc.getDutyTel3())
                            .distanceKm(Math.round(distance * 10.0) / 10.0)
                            .availableBeds(Math.max(availableBeds, 0))
                            .hospitalType(loc.getDgidIdName())
                            .latitude(loc.getWgs84Lat())
                            .longitude(loc.getWgs84Lon())
                            .build();
                })
                .sorted(Comparator.comparingDouble(HospitalRankItem::getScore).reversed())
                .limit(MAX_RESULTS)
                .toList();

        List<HospitalRankItem> ranked = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            HospitalRankItem item = sorted.get(i);
            ranked.add(HospitalRankItem.builder()
                    .rank(i + 1)
                    .score(item.getScore())
                    .hpid(item.getHpid())
                    .hospitalName(item.getHospitalName())
                    .address(item.getAddress())
                    .tel(item.getTel())
                    .emergencyTel(item.getEmergencyTel())
                    .distanceKm(item.getDistanceKm())
                    .availableBeds(item.getAvailableBeds())
                    .hospitalType(item.getHospitalType())
                    .latitude(item.getLatitude())
                    .longitude(item.getLongitude())
                    .build());
        }

        return HospitalSearchResponse.builder()
                .totalCount(ranked.size())
                .ktasApplied(ktasApplied)
                .hospitals(ranked)
                .build();
    }

    public HospitalDetailResponse getHospitalDetail(String hpid) {
        HospitalBasicInfoItem info = emergencyApiClient.getHospitalBasicInfo(hpid);

        return HospitalDetailResponse.builder()
                .hpid(info.getHpid())
                .hospitalName(info.getDutyName())
                .address(info.getDutyAddr())
                .tel(info.getDutyTel1())
                .emergencyTel(info.getDutyTel3())
                .hospitalType(info.getDgidIdName())
                .emergencyOperating(info.getDutyEryn())
                .hospitalizationAvailable(info.getDutyHayn())
                .bedCount(info.getDutyHano())
                .departments(info.getDutyInf())
                .latitude(info.getWgs84Lat())
                .longitude(info.getWgs84Lon())
                .bedInfo(HospitalDetailResponse.BedInfo.builder()
                        .emergencyBeds(info.getHperyn())
                        .operatingRooms(info.getHpopyn())
                        .icuBeds(info.getHpicuyn())
                        .neonatalIcuBeds(info.getHpnicuyn())
                        .generalBeds(info.getHpbdn())
                        .surgicalBeds(info.getHpgryn())
                        .build())
                .build();
    }

    private String resolveRegion(double lat, double lon) {
        double minDist = Double.MAX_VALUE;
        int minIdx = 0;

        for (int i = 0; i < REGION_CENTERS.length; i++) {
            double dist = calculateHaversineDistance(lat, lon, REGION_CENTERS[i][0], REGION_CENTERS[i][1]);
            if (dist < minDist) {
                minDist = dist;
                minIdx = i;
            }
        }

        return REGION_NAMES[minIdx];
    }

    private Map<String, BedAvailabilityItem> fetchBedAvailability(String stage1, String stage2) {
        try {
            List<BedAvailabilityItem> beds = emergencyApiClient.getBedAvailability(stage1, stage2);
            return beds.stream()
                    .collect(Collectors.toMap(BedAvailabilityItem::getHpid, b -> b, (a, b) -> a));
        } catch (Exception e) {
            log.warn("병상 정보 조회 실패, 거리/KTAS만으로 점수 산출: {}", e.getMessage());
            return Map.of();
        }
    }

    private double calculateScore(double distanceKm, int availableBeds, String hospitalType, Integer ktasLevel) {
        double distanceScore = Math.exp(-DISTANCE_DECAY_RATE * distanceKm);
        double bedScore = Math.min(availableBeds / MAX_BED_COUNT, 1.0);

        if (ktasLevel != null) {
            double ktasScore = calculateKtasScore(ktasLevel, hospitalType);
            return WEIGHT_DISTANCE_WITH_KTAS * distanceScore
                    + WEIGHT_BED_WITH_KTAS * bedScore
                    + WEIGHT_KTAS * ktasScore;
        }

        return WEIGHT_DISTANCE_WITHOUT_KTAS * distanceScore
                + WEIGHT_BED_WITHOUT_KTAS * bedScore;
    }

    private double calculateKtasScore(int ktasLevel, String hospitalType) {
        if (hospitalType == null) {
            return 0.2;
        }

        boolean isRegionalCenter = hospitalType.contains("권역");
        boolean isLocalCenter = hospitalType.contains("지역") && hospitalType.contains("센터");
        boolean isLocalInstitution = hospitalType.contains("지역") && hospitalType.contains("기관");

        if (ktasLevel <= 2) {
            if (isRegionalCenter) return 1.0;
            if (isLocalCenter) return 0.5;
            return 0.2;
        } else if (ktasLevel == 3) {
            if (isLocalCenter) return 1.0;
            if (isRegionalCenter || isLocalInstitution) return 0.5;
            return 0.2;
        } else {
            if (isLocalInstitution) return 1.0;
            if (isLocalCenter) return 0.5;
            return 0.2;
        }
    }

    static double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
