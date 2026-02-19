package com.example.aegis_be.domain.report.entity;

import com.example.aegis_be.domain.dispatch.entity.DispatchSession;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ambulance_report",
        indexes = {
                @Index(name = "idx_report_session", columnList = "dispatch_session_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AmbulanceReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispatch_session_id", nullable = false, unique = true)
    private DispatchSession dispatchSession;

    // === AI 제공 필드 ===

    @Convert(converter = IntegerListConverter.class)
    @Column(name = "ai_checklist_data", columnDefinition = "TEXT")
    private List<Integer> aiChecklistData;

    @Column(name = "sbp")
    private Integer sbp;

    @Column(name = "dbp")
    private Integer dbp;

    @Column(name = "rr")
    private Integer rr;

    @Column(name = "pr")
    private Integer pr;

    @Column(name = "temp_c")
    private Double tempC;

    @Column(name = "sp_o2")
    private Integer spO2;

    @Column(name = "glucose")
    private Integer glucose;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    // === 구급대원 확정 필드 ===

    @Convert(converter = IntegerListConverter.class)
    @Column(name = "checklist_data", columnDefinition = "TEXT")
    private List<Integer> checklistData;

    @Column(name = "chief_complaint", columnDefinition = "TEXT")
    private String chiefComplaint;

    @Column(name = "assessment", columnDefinition = "TEXT")
    private String assessment;

    @Column(name = "incident_date_time")
    private LocalDateTime incidentDateTime;

    // === 시각 ===

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public AmbulanceReport(DispatchSession dispatchSession) {
        this.dispatchSession = dispatchSession;
        this.createdAt = LocalDateTime.now();
    }

    // AI: 체크리스트
    public void updateAiChecklist(List<Integer> aiChecklistData) {
        this.aiChecklistData = aiChecklistData;
    }

    // AI: 바이탈 (OCR)
    public void updateVitals(Integer sbp, Integer dbp, Integer rr, Integer pr,
                             Double tempC, Integer spO2, Integer glucose) {
        this.sbp = sbp;
        this.dbp = dbp;
        this.rr = rr;
        this.pr = pr;
        this.tempC = tempC;
        this.spO2 = spO2;
        this.glucose = glucose;
    }

    // AI: 텍스트 요약
    public void updateSummary(String summary) {
        this.summary = summary;
    }

    // 구급대원: 체크리스트 확정/수정
    public void updateChecklist(List<Integer> checklistData) {
        this.checklistData = checklistData;
    }

    // 구급대원: 주호소 + 평가소견 + 발생일시
    public void updateAssessment(String chiefComplaint, String assessment, LocalDateTime incidentDateTime) {
        this.chiefComplaint = chiefComplaint;
        this.assessment = assessment;
        this.incidentDateTime = incidentDateTime;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
