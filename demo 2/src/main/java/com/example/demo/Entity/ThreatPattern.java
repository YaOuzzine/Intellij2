package com.example.demo.Entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "threat_patterns", schema = "gateway")
public class ThreatPattern {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pattern_name", unique = true, nullable = false)
    private String patternName;

    @Column(name = "pattern_type", nullable = false)
    private String patternType; // IP_FREQUENCY, PATH_PATTERN, USER_AGENT, GEOGRAPHIC, TEMPORAL

    @Column(name = "pattern_definition")
    @JdbcTypeCode(SqlTypes.JSON)
    private String patternDefinition; // JSON configuration for the pattern

    @Column(name = "threat_level", nullable = false)
    private String threatLevel; // LOW, MEDIUM, HIGH, CRITICAL

    @Column(name = "confidence_threshold", nullable = false)
    private Double confidenceThreshold = 0.8; // 0.0 to 1.0

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "auto_block", nullable = false)
    private Boolean autoBlock = false; // Whether to automatically block matching traffic

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_triggered")
    private LocalDateTime lastTriggered;

    @Column(name = "trigger_count", nullable = false)
    private Long triggerCount = 0L;

    @Column(name = "false_positive_count", nullable = false)
    private Long falsePositiveCount = 0L;

    @Column(name = "description")
    private String description;

    // Constructors
    public ThreatPattern() {
        this.createdAt = LocalDateTime.now();
    }

    public ThreatPattern(String patternName, String patternType, String threatLevel) {
        this();
        this.patternName = patternName;
        this.patternType = patternType;
        this.threatLevel = threatLevel;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPatternName() { return patternName; }
    public void setPatternName(String patternName) { this.patternName = patternName; }

    public String getPatternType() { return patternType; }
    public void setPatternType(String patternType) { this.patternType = patternType; }

    public String getPatternDefinition() { return patternDefinition; }
    public void setPatternDefinition(String patternDefinition) { this.patternDefinition = patternDefinition; }

    public String getThreatLevel() { return threatLevel; }
    public void setThreatLevel(String threatLevel) { this.threatLevel = threatLevel; }

    public Double getConfidenceThreshold() { return confidenceThreshold; }
    public void setConfidenceThreshold(Double confidenceThreshold) { this.confidenceThreshold = confidenceThreshold; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Boolean getAutoBlock() { return autoBlock; }
    public void setAutoBlock(Boolean autoBlock) { this.autoBlock = autoBlock; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getLastTriggered() { return lastTriggered; }
    public void setLastTriggered(LocalDateTime lastTriggered) { this.lastTriggered = lastTriggered; }

    public Long getTriggerCount() { return triggerCount; }
    public void setTriggerCount(Long triggerCount) { this.triggerCount = triggerCount; }

    public Long getFalsePositiveCount() { return falsePositiveCount; }
    public void setFalsePositiveCount(Long falsePositiveCount) { this.falsePositiveCount = falsePositiveCount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementTriggerCount() {
        this.triggerCount++;
        this.lastTriggered = LocalDateTime.now();
    }
}