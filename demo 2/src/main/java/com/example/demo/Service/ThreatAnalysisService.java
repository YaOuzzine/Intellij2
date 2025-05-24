package com.example.demo.Service;

import com.example.demo.Entity.SecurityEvent;
import com.example.demo.Entity.ThreatAlert;
import com.example.demo.Entity.ThreatPattern;
import com.example.demo.Repository.SecurityEventRepository;
import com.example.demo.Repository.ThreatAlertRepository;
import com.example.demo.Repository.ThreatPatternRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class ThreatAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ThreatAnalysisService.class);

    private final SecurityEventRepository eventRepository;
    private final ThreatPatternRepository patternRepository;
    private final ThreatAlertRepository alertRepository;
    private final ObjectMapper objectMapper;

    // Thresholds for anomaly detection
    private static final int HIGH_FREQUENCY_THRESHOLD = 100; // requests per minute
    private static final int SUSPICIOUS_IP_THRESHOLD = 50;   // rejections per hour
    private static final double ANOMALY_SCORE_THRESHOLD = 0.8;
    private static final long PATTERN_ANALYSIS_WINDOW_HOURS = 24;

    @Autowired
    public ThreatAnalysisService(SecurityEventRepository eventRepository,
                                 ThreatPatternRepository patternRepository,
                                 ThreatAlertRepository alertRepository) {
        this.eventRepository = eventRepository;
        this.patternRepository = patternRepository;
        this.alertRepository = alertRepository;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void initializeDefaultPatterns() {
        log.info("Initializing default threat patterns...");
        createDefaultThreatPatterns();
    }

    /**
     * Analyze a security event for potential threats
     */
    @Async
    @Transactional
    public CompletableFuture<List<ThreatAlert>> analyzeEvent(SecurityEvent event) {
        List<ThreatAlert> alerts = new ArrayList<>();

        try {
            log.debug("Analyzing security event: type={}, clientIp={}, routeId={}",
                    event.getEventType(), event.getClientIp(), event.getRouteId());

            // Check against known patterns
            List<ThreatPattern> activePatterns = patternRepository.findByIsActiveTrue();
            for (ThreatPattern pattern : activePatterns) {
                double confidence = evaluatePatternMatch(event, pattern);
                if (confidence >= pattern.getConfidenceThreshold()) {
                    ThreatAlert alert = createThreatAlert(event, pattern, confidence);
                    alerts.add(alert);

                    // Update pattern statistics
                    pattern.incrementTriggerCount();
                    patternRepository.save(pattern);

                    log.info("Threat pattern match: {} (confidence: {:.2f})",
                            pattern.getPatternName(), confidence);
                }
            }

            // Perform anomaly detection
            ThreatAlert anomalyAlert = detectAnomalies(event);
            if (anomalyAlert != null) {
                alerts.add(anomalyAlert);
            }

            // Save all alerts
            if (!alerts.isEmpty()) {
                alertRepository.saveAll(alerts);
                log.warn("Generated {} threat alerts for event from IP: {}",
                        alerts.size(), event.getClientIp());
            }

        } catch (Exception e) {
            log.error("Error analyzing security event: {}", e.getMessage(), e);
        }

        return CompletableFuture.completedFuture(alerts);
    }

    /**
     * Scheduled analysis of traffic patterns
     */
    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    @Transactional
    public void analyzeTrafficPatterns() {
        try {
            LocalDateTime analysisStart = LocalDateTime.now().minusHours(1);
            log.debug("Starting traffic pattern analysis from {}", analysisStart);

            // Analyze high-frequency sources
            analyzeHighFrequencySources(analysisStart);

            // Analyze geographic anomalies
            analyzeGeographicAnomalies(analysisStart);

            // Analyze temporal patterns
            analyzeTemporalPatterns(analysisStart);

            // Update threat scores
            updateThreatScores();

        } catch (Exception e) {
            log.error("Error in scheduled traffic pattern analysis: {}", e.getMessage(), e);
        }
    }

    /**
     * Get current threat landscape summary
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getThreatLandscape() {
        Map<String, Object> landscape = new HashMap<>();

        try {
            LocalDateTime since = LocalDateTime.now().minusHours(24);

            // Active alerts
            Long openAlerts = alertRepository.countOpenAlerts();
            List<ThreatAlert> criticalAlerts = alertRepository.findActiveCriticalAlerts();

            // Alert distribution
            List<Object[]> severityDistribution = alertRepository.getAlertSeverityDistribution(since);

            // Top threat patterns
            List<ThreatPattern> topPatterns = patternRepository.findRecentlyTriggeredPatterns(since)
                    .stream().limit(10).collect(Collectors.toList());

            // Suspicious activity stats
            Map<String, Object> suspiciousActivity = getSuspiciousActivityStats(since);

            landscape.put("openAlerts", openAlerts);
            landscape.put("criticalAlerts", criticalAlerts.size());
            landscape.put("severityDistribution", severityDistribution);
            landscape.put("topThreatPatterns", topPatterns);
            landscape.put("suspiciousActivity", suspiciousActivity);
            landscape.put("lastUpdated", LocalDateTime.now());

            log.debug("Generated threat landscape: {} open alerts, {} critical alerts",
                    openAlerts, criticalAlerts.size());

        } catch (Exception e) {
            log.error("Error generating threat landscape: {}", e.getMessage(), e);
        }

        return landscape;
    }

    /**
     * Get security recommendations based on current threats
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getSecurityRecommendations() {
        List<Map<String, Object>> recommendations = new ArrayList<>();

        try {
            LocalDateTime since = LocalDateTime.now().minusHours(24);

            // Check for high-volume attacking IPs
            List<Object[]> topAttackers = eventRepository.findTopAttackingIPs(since);
            if (!topAttackers.isEmpty()) {
                Map<String, Object> recommendation = new HashMap<>();
                recommendation.put("type", "IP_BLOCKING");
                recommendation.put("priority", "HIGH");
                recommendation.put("title", "Consider blocking high-volume attacking IPs");
                recommendation.put("description", String.format("Found %d IPs with suspicious activity", topAttackers.size()));
                recommendation.put("affectedIPs", topAttackers.stream().limit(5).collect(Collectors.toList()));
                recommendations.add(recommendation);
            }

            // Check for unusual traffic patterns
            analyzeTrafficVolumeTrends(recommendations, since);

            // Check for outdated threat patterns
            analyzePatternEffectiveness(recommendations);

            // Check for response time anomalies
            analyzeResponseTimeAnomalies(recommendations, since);

        } catch (Exception e) {
            log.error("Error generating security recommendations: {}", e.getMessage(), e);
        }

        return recommendations;
    }

    /**
     * Create a threat alert from an event and pattern match
     */
    private ThreatAlert createThreatAlert(SecurityEvent event, ThreatPattern pattern, double confidence) {
        // Check if similar alert already exists
        Optional<ThreatAlert> existingAlert = alertRepository
                .findBySourceIpAndTargetRouteAndStatus(event.getClientIp(), event.getRouteId(), "OPEN");

        if (existingAlert.isPresent()) {
            // Update existing alert
            ThreatAlert alert = existingAlert.get();
            alert.incrementEventCount();
            alert.setConfidence(Math.max(alert.getConfidence(), confidence));
            return alert;
        } else {
            // Create new alert
            ThreatAlert alert = new ThreatAlert("PATTERN_MATCH", pattern.getThreatLevel(),
                    "Threat Pattern Detected: " + pattern.getPatternName());
            alert.setDescription(pattern.getDescription());
            alert.setSourceIp(event.getClientIp());
            alert.setTargetRoute(event.getRouteId());
            alert.setConfidence(confidence);
            alert.setThreatScore(calculateThreatScore(event, pattern));
            alert.setRelatedPatternId(pattern.getId());

            // Add metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("patternType", pattern.getPatternType());
            metadata.put("userAgent", event.getUserAgent());
            metadata.put("requestPath", event.getRequestPath());
            try {
                alert.setMetadata(objectMapper.writeValueAsString(metadata));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize alert metadata: {}", e.getMessage());
            }

            return alert;
        }
    }

    /**
     * Evaluate how well an event matches a threat pattern
     */
    private double evaluatePatternMatch(SecurityEvent event, ThreatPattern pattern) {
        try {
            Map<String, Object> patternDef = objectMapper.readValue(
                    pattern.getPatternDefinition(), Map.class);

            double score = 0.0;
            int checks = 0;

            // IP frequency pattern
            if ("IP_FREQUENCY".equals(pattern.getPatternType())) {
                score += evaluateIpFrequencyPattern(event, patternDef);
                checks++;
            }

            // Path pattern matching
            if ("PATH_PATTERN".equals(pattern.getPatternType())) {
                score += evaluatePathPattern(event, patternDef);
                checks++;
            }

            // User agent pattern
            if ("USER_AGENT".equals(pattern.getPatternType())) {
                score += evaluateUserAgentPattern(event, patternDef);
                checks++;
            }

            // Temporal pattern
            if ("TEMPORAL".equals(pattern.getPatternType())) {
                score += evaluateTemporalPattern(event, patternDef);
                checks++;
            }

            return checks > 0 ? score / checks : 0.0;

        } catch (Exception e) {
            log.warn("Error evaluating pattern match for pattern {}: {}",
                    pattern.getPatternName(), e.getMessage());
            return 0.0;
        }
    }

    /**
     * Detect anomalies in the current event
     */
    private ThreatAlert detectAnomalies(SecurityEvent event) {
        try {
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

            // Check for unusual frequency from this IP
            List<SecurityEvent> recentEvents = eventRepository
                    .findByClientIpAndTimestampBetween(event.getClientIp(), oneHourAgo, LocalDateTime.now());

            if (recentEvents.size() > HIGH_FREQUENCY_THRESHOLD) {
                ThreatAlert alert = new ThreatAlert("ANOMALY", "HIGH", "High Frequency Anomaly Detected");
                alert.setDescription(String.format("IP %s made %d requests in the last hour",
                        event.getClientIp(), recentEvents.size()));
                alert.setSourceIp(event.getClientIp());
                alert.setTargetRoute(event.getRouteId());
                alert.setThreatScore(Math.min(1.0, recentEvents.size() / (double)HIGH_FREQUENCY_THRESHOLD));
                alert.setConfidence(0.9);
                return alert;
            }

        } catch (Exception e) {
            log.warn("Error in anomaly detection: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Calculate threat score for an event/pattern combination
     */
    private double calculateThreatScore(SecurityEvent event, ThreatPattern pattern) {
        double baseScore = switch (pattern.getThreatLevel()) {
            case "LOW" -> 0.2;
            case "MEDIUM" -> 0.5;
            case "HIGH" -> 0.8;
            case "CRITICAL" -> 1.0;
            default -> 0.1;
        };

        // Adjust based on rejection type
        if ("REJECTION".equals(event.getEventType())) {
            baseScore += 0.2;
        }

        // Adjust based on response time
        if (event.getResponseTimeMs() != null && event.getResponseTimeMs() > 5000) {
            baseScore += 0.1;
        }

        return Math.min(1.0, baseScore);
    }

    /**
     * Initialize default threat patterns
     */
    private void createDefaultThreatPatterns() {
        createPatternIfNotExists("SQL_INJECTION_ATTEMPT", "PATH_PATTERN", "HIGH",
                "{\"patterns\": [\"SELECT \", \"UNION \", \"DROP \", \"INSERT \", \"UPDATE \", \"DELETE \"], \"caseSensitive\": false}",
                "Detects potential SQL injection attempts in request paths");

        createPatternIfNotExists("XSS_ATTEMPT", "PATH_PATTERN", "HIGH",
                "{\"patterns\": [\"<script\", \"javascript:\", \"onerror=\", \"onload=\"], \"caseSensitive\": false}",
                "Detects potential Cross-Site Scripting (XSS) attempts");

        createPatternIfNotExists("SCANNER_USER_AGENT", "USER_AGENT", "MEDIUM",
                "{\"patterns\": [\"sqlmap\", \"nikto\", \"nmap\", \"masscan\", \"burp\", \"zap\"], \"caseSensitive\": false}",
                "Detects known security scanning tools");

        createPatternIfNotExists("HIGH_FREQUENCY_IP", "IP_FREQUENCY", "MEDIUM",
                "{\"requestsPerMinute\": 50, \"timeWindowMinutes\": 5}",
                "Detects IPs making unusually high number of requests");
    }

    private void createPatternIfNotExists(String name, String type, String threatLevel,
                                          String definition, String description) {
        if (patternRepository.findByIsActiveTrue().stream()
                .noneMatch(p -> p.getPatternName().equals(name))) {
            ThreatPattern pattern = new ThreatPattern(name, type, threatLevel);
            pattern.setPatternDefinition(definition);
            pattern.setDescription(description);
            patternRepository.save(pattern);
            log.info("Created default threat pattern: {}", name);
        }
    }

    // Helper methods for specific pattern evaluations
    private double evaluateIpFrequencyPattern(SecurityEvent event, Map<String, Object> patternDef) {
        // Implementation for IP frequency analysis
        return 0.0; // Simplified for now
    }

    private double evaluatePathPattern(SecurityEvent event, Map<String, Object> patternDef) {
        if (event.getRequestPath() == null) return 0.0;

        @SuppressWarnings("unchecked")
        List<String> patterns = (List<String>) patternDef.get("patterns");
        if (patterns == null) return 0.0;

        String path = event.getRequestPath().toLowerCase();
        for (String pattern : patterns) {
            if (path.contains(pattern.toLowerCase())) {
                return 1.0;
            }
        }
        return 0.0;
    }

    private double evaluateUserAgentPattern(SecurityEvent event, Map<String, Object> patternDef) {
        if (event.getUserAgent() == null) return 0.0;

        @SuppressWarnings("unchecked")
        List<String> patterns = (List<String>) patternDef.get("patterns");
        if (patterns == null) return 0.0;

        String userAgent = event.getUserAgent().toLowerCase();
        for (String pattern : patterns) {
            if (userAgent.contains(pattern.toLowerCase())) {
                return 1.0;
            }
        }
        return 0.0;
    }

    private double evaluateTemporalPattern(SecurityEvent event, Map<String, Object> patternDef) {
        // Implementation for temporal analysis
        return 0.0; // Simplified for now
    }

    // Additional analysis methods
    private void analyzeHighFrequencySources(LocalDateTime since) {
        // Implementation for high frequency analysis
    }

    private void analyzeGeographicAnomalies(LocalDateTime since) {
        // Implementation for geographic analysis
    }

    private void analyzeTemporalPatterns(LocalDateTime since) {
        // Implementation for temporal analysis
    }

    private void updateThreatScores() {
        // Implementation for threat score updates
    }

    private Map<String, Object> getSuspiciousActivityStats(LocalDateTime since) {
        Map<String, Object> stats = new HashMap<>();
        List<Object[]> suspiciousIPs = eventRepository.findSuspiciousIPs(since, 10L);
        stats.put("suspiciousIPCount", suspiciousIPs.size());
        stats.put("topSuspiciousIPs", suspiciousIPs.stream().limit(5).collect(Collectors.toList()));
        return stats;
    }

    private void analyzeTrafficVolumeTrends(List<Map<String, Object>> recommendations, LocalDateTime since) {
        // Implementation for traffic volume analysis
    }

    private void analyzePatternEffectiveness(List<Map<String, Object>> recommendations) {
        // Implementation for pattern effectiveness analysis
    }

    private void analyzeResponseTimeAnomalies(List<Map<String, Object>> recommendations, LocalDateTime since) {
        // Implementation for response time analysis
    }
}