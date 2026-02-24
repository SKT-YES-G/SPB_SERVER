package com.example.aegis_be.domain.medical.service;

import com.example.aegis_be.domain.eventlog.entity.EventLog;
import com.example.aegis_be.domain.eventlog.entity.EventType;
import com.example.aegis_be.domain.eventlog.service.EventLogService;
import com.example.aegis_be.domain.medical.client.EmergencyApiClient;
import com.example.aegis_be.domain.medical.client.dto.BedAvailabilityItem;
import com.example.aegis_be.domain.medical.client.dto.HospitalLocationItem;
import com.example.aegis_be.domain.medical.dto.HospitalRankItem;
import com.example.aegis_be.domain.medical.dto.HospitalSearchRequest;
import com.example.aegis_be.domain.medical.dto.HospitalSearchResponse;
import com.example.aegis_be.domain.prektas.repository.PreKtasRepository;
import com.example.aegis_be.global.client.AiApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MedicalMapService {

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double DISTANCE_DECAY_RATE = 0.1;
    private static final double BED_SATURATION_COUNT = 5.0;
    private static final int MAX_RESULTS = 15;
    private static final int DEPT_FETCH_LIMIT = 30;
    private static final int NEARBY_REGION_COUNT = 2;

    private static final double WEIGHT_DISTANCE_WITH_KTAS = 0.5;
    private static final double WEIGHT_BED_WITH_KTAS = 0.15;
    private static final double WEIGHT_KTAS = 0.35;

    private static final double WEIGHT_DISTANCE_WITHOUT_KTAS = 0.8;
    private static final double WEIGHT_BED_WITHOUT_KTAS = 0.2;

    private static final double[][] REGION_CENTERS = {
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
    private final PreKtasRepository preKtasRepository;
    private final EventLogService eventLogService;
    private final AiApiClient aiApiClient;

    public HospitalSearchResponse searchHospitals(HospitalSearchRequest request) {
        Integer ktasLevel = request.getKtasLevel();

        List<String> regions = resolveNearbyRegions(request.getLatitude(), request.getLongitude());
        log.info("좌표({}, {}) → 인접 시도: {}, KTAS: {}", request.getLatitude(), request.getLongitude(), regions, ktasLevel);

        List<HospitalLocationItem> hospitals = new ArrayList<>();
        Map<String, BedAvailabilityItem> bedMap = new HashMap<>();

        for (String region : regions) {
            try {
                hospitals.addAll(emergencyApiClient.getHospitalList(region, null));
                bedMap.putAll(fetchBedAvailability(region, null));
            } catch (Exception e) {
                log.warn("시도 '{}' 조회 실패, 건너뜀: {}", region, e.getMessage());
            }
        }

        boolean ktasApplied = ktasLevel != null;

        // sessionId가 있으면 AI 진료과 자동 조회
        List<String> departments = request.getSessionId() != null
                ? resolveAiDepartments(request.getSessionId())
                : null;

        final List<String> finalDepartments = departments;
        boolean deptFilterActive = finalDepartments != null && !finalDepartments.isEmpty();

        // 중복 제거 + Filter 0: 응급실 운영 중(hvamyn)인 병원만
        List<HospitalLocationItem> filtered = hospitals.stream()
                .collect(Collectors.toMap(HospitalLocationItem::getHpid, h -> h, (a, b) -> a))
                .values().stream()
                .filter(loc -> {
                    BedAvailabilityItem bed = bedMap.get(loc.getHpid());
                    if (bed == null || !"Y".equalsIgnoreCase(bed.getHvamyn())) {
                        return false;
                    }
                    int available = Math.max(bed.getHvec(), 0) + Math.max(bed.getHvicc(), 0) + Math.max(bed.getHvgc(), 0);
                    return available > 0;
                })
                .toList();

        log.info("Filter 0 (응급실 운영) 통과: {}개 병원", filtered.size());

        // Filter 1: 진료과 매칭 (departments 입력 시에만)
        // 진료과(dutyInf)는 상세 API에서만 제공 → 병원별 순차 조회 (rate limit 회피)
        // 효율화: 거리순 상위 DEPT_FETCH_LIMIT개만 진료과 조회
        Map<String, String> departmentMap = new HashMap<>();
        if (deptFilterActive) {
            List<HospitalLocationItem> candidates = filtered.stream()
                    .sorted(Comparator.comparingDouble(loc ->
                            calculateHaversineDistance(request.getLatitude(), request.getLongitude(),
                                    loc.getWgs84Lat(), loc.getWgs84Lon())))
                    .limit(DEPT_FETCH_LIMIT)
                    .toList();

            for (HospitalLocationItem loc : candidates) {
                try {
                    String dutyInf = emergencyApiClient.fetchHospitalDepartments(loc.getHpid());
                    if (dutyInf != null) {
                        departmentMap.put(loc.getHpid(), dutyInf);
                    }
                } catch (Exception e) {
                    log.warn("진료과 조회 실패 ({}): {}", loc.getHpid(), e.getMessage());
                }
            }

            filtered = filtered.stream()
                    .filter(loc -> {
                        String dutyInf = departmentMap.get(loc.getHpid());
                        if (dutyInf == null || dutyInf.isBlank()) {
                            return false;
                        }
                        return finalDepartments.stream().anyMatch(dutyInf::contains);
                    })
                    .toList();

            log.info("Filter 1 (진료과 매칭) 통과: {}개 병원", filtered.size());
        }

        // 점수 산출 및 순위
        List<HospitalRankItem> sorted = filtered.stream()
                .map(loc -> {
                    double distance = calculateHaversineDistance(
                            request.getLatitude(), request.getLongitude(),
                            loc.getWgs84Lat(), loc.getWgs84Lon());

                    BedAvailabilityItem bed = bedMap.get(loc.getHpid());
                    int hvec = (bed != null) ? Math.max(bed.getHvec(), 0) : 0;
                    int hvicc = (bed != null) ? Math.max(bed.getHvicc(), 0) : 0;
                    int hvgc = (bed != null) ? Math.max(bed.getHvgc(), 0) : 0;
                    int availableBeds = hvec + hvicc + hvgc;

                    double score = calculateScore(distance, availableBeds, loc.getDgidIdName(),
                            ktasLevel);

                    String depts = departmentMap.getOrDefault(loc.getHpid(), loc.getDutyInf());

                    return HospitalRankItem.builder()
                            .score(Math.round(score * 100.0) / 100.0)
                            .hpid(loc.getHpid())
                            .hospitalName(loc.getDutyName())
                            .distanceKm(Math.round(distance * 10.0) / 10.0)
                            .emergencyBeds(hvec)
                            .icuBeds(hvicc)
                            .inpatientBeds(hvgc)
                            .availableBeds(availableBeds)
                            .hospitalType(loc.getDgidIdName())
                            .departments(depts)
                            .address(loc.getDutyAddr())
                            .mainTel(loc.getDutyTel1())
                            .emergencyTel(loc.getDutyTel3())
                            .dutyTel(bed != null ? bed.getHv1() : null)
                            .latitude(loc.getWgs84Lat())
                            .longitude(loc.getWgs84Lon())
                            .build();
                })
                .sorted(Comparator.comparingDouble(HospitalRankItem::getScore).reversed())
                .limit(MAX_RESULTS)
                .toList();

        // 최종 결과에 대해 진료과 정보 보강 (아직 조회 안 된 병원만)
        for (HospitalRankItem item : sorted) {
            if (!departmentMap.containsKey(item.getHpid())) {
                try {
                    String dutyInf = emergencyApiClient.fetchHospitalDepartments(item.getHpid());
                    if (dutyInf != null) {
                        departmentMap.put(item.getHpid(), dutyInf);
                    }
                } catch (Exception e) {
                    log.warn("진료과 보강 조회 실패 ({}): {}", item.getHpid(), e.getMessage());
                }
            }
        }

        List<HospitalRankItem> ranked = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            HospitalRankItem item = sorted.get(i);
            ranked.add(item.toBuilder()
                    .rank(i + 1)
                    .departments(departmentMap.get(item.getHpid()))
                    .build());
        }

        return HospitalSearchResponse.builder()
                .totalCount(ranked.size())
                .ktasApplied(ktasApplied)
                .recommendedDepartments(finalDepartments)
                .hospitals(ranked)
                .build();
    }

    private List<String> resolveNearbyRegions(double lat, double lon) {
        record RegionDist(int index, double distance) {}

        List<RegionDist> distances = new ArrayList<>();
        for (int i = 0; i < REGION_CENTERS.length; i++) {
            double dist = calculateHaversineDistance(lat, lon, REGION_CENTERS[i][0], REGION_CENTERS[i][1]);
            distances.add(new RegionDist(i, dist));
        }

        return distances.stream()
                .sorted(Comparator.comparingDouble(RegionDist::distance))
                .limit(NEARBY_REGION_COUNT)
                .map(rd -> REGION_NAMES[rd.index()])
                .toList();
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
        double bedScore = Math.min(availableBeds / BED_SATURATION_COUNT, 1.0);

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

    private List<String> resolveAiDepartments(Long sessionId) {
        // 1. PreKtas에 이미 저장된 진료과가 있으면 바로 반환
        var preKtasOpt = preKtasRepository.findByDispatchSessionId(sessionId);
        if (preKtasOpt.isPresent()) {
            List<String> existing = preKtasOpt.get().getAiDepartments();
            if (existing != null && !existing.isEmpty()) {
                log.info("세션 {} PreKtas에 저장된 AI 진료과 사용: {}", sessionId, existing);
                return existing;
            }
        }

        // 2. 없으면 이벤트 로그의 판단근거 더미로 AI API 호출
        try {
            List<EventLog> reasoningLogs = eventLogService.findBySessionIdAndEventType(sessionId, EventType.AI_REASONING_SAVED);
            if (reasoningLogs.isEmpty()) {
                log.info("세션 {} 에 AI_REASONING_SAVED 이벤트 없음, 진료과 추천 건너뜀", sessionId);
                return null;
            }

            String combinedText = reasoningLogs.stream()
                    .map(EventLog::getDescription)
                    .collect(Collectors.joining("\n"));

            log.info("세션 {} AI 진료과 추천 요청, reasoning 로그 {}건, 텍스트 길이: {}", sessionId, reasoningLogs.size(), combinedText.length());

            List<String> departments = aiApiClient.requestDepartmentRecommendation(combinedText);
            log.info("세션 {} AI 추천 진료과: {}", sessionId, departments);

            // PreKtas에 명시적 save (트랜잭션 밖이므로 dirty checking 불가)
            preKtasOpt.ifPresent(preKtas -> {
                preKtas.updateAiDepartments(departments);
                preKtasRepository.save(preKtas);
            });

            return departments;
        } catch (Exception e) {
            log.warn("세션 {} AI 진료과 추천 실패, 진료과 필터 없이 진행: {}", sessionId, e.getMessage());
            return null;
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
