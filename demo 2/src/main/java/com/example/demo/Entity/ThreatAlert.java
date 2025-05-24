package com.example.demo.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "threat_alerts", schema = "gateway")
public class ThreatAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_type", nullable = false)
    private String alertType; // ANOMALY, PATTERN_MATCH, THRESHOLD_BREACH, MANUAL

    @Column(name = "severity", nullable = false)
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "source_ip")
    private String sourceIp;

    @Column(name = "target_route")
    private String targetRoute;

    @Column(name = "threat_score", nullable = false)
    private Double threatScore; // 0.0 to 1.0

    @Column(name = "confidence", nullable = false)
    private Double confidence; // 0.0 to 1.0

    @Column(name = "event_count", nullable = false)
    private Long eventCount = 1L;

    @Column(name = "first_seen", nullable = false)
    private LocalDateTime firstSeen;

    @Column(name = "last_seen", nullable = false)
    private LocalDateTime lastSeen;

    @Column(name = "status", nullable = false)
    private String status = "OPEN"; // OPEN, INVESTIGATING, RESOLVED, FALSE_POSITIVE

    @Column(name = "assigned_to")
    private String assignedTo;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "auto_resolved", nullable = false)
    private Boolean autoResolved = false;

    @Column(name = "related_pattern_id")
    private Long relatedPatternId;

    @Column(name = "metadata", columnDefinition = "JSONB")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public ThreatAlert() {
        this.createdAt = LocalDateTime.now();
        this.firstSeen = LocalDateTime.now();
        this.lastSeen = LocalDateTime.now();
    }

    public ThreatAlert(String alertType, String severity, String title) {
        this();
        this.alertType = alertType;
        this.severity = severity;
        this.title = title;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSourceIp() { return sourceIp; }
    public void setSourceIp(String sourceIp) { this.sourceIp = sourceIp; }

    public String getTargetRoute() { return targetRoute; }
    public void setTargetRoute(String targetRoute) { this.targetRoute = targetRoute; }

    public Double getThreatScore() { return threatScore; }
    public void setThreatScore(Double threatScore) { this.threatScore = threatScore; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public Long getEventCount() { return eventCount; }
    public void setEventCount(Long eventCount) { this.eventCount = eventCount; }

    public LocalDateTime getFirstSeen() { return firstSeen; }
    public void setFirstSeen(LocalDateTime firstSeen) { this.firstSeen = firstSeen; }

    public LocalDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }

    public Boolean getAutoResolved() { return autoResolved; }
    public void setAutoResolved(Boolean autoResolved) { this.autoResolved = autoResolved; }

    public Long getRelatedPatternId() { return relatedPatternId; }
    public void setRelatedPatternId(Long relatedPatternId) { this.relatedPatternId = relatedPatternId; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementEventCount() {
        this.eventCount++;
        this.lastSeen = LocalDateTime.now();
    }
}