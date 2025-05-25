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

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
    private static final int TEMPORAL_ANALYSIS_MINUTES = 60;
    private static final double RESPONSE_TIME_ANOMALY_MULTIPLIER = 3.0;

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

        createPatternIfNotExists("NIGHT_TIME_ACTIVITY", "TEMPORAL", "MEDIUM",
                "{\"suspiciousHours\": [22, 23, 0, 1, 2, 3, 4, 5], \"timezone\": \"UTC\"}",
                "Detects unusual activity during night hours");
    }

    private void createPatternIfNotExists(String name, String type, String threatLevel,
                                          String definition, String description) {
        try {
            List<ThreatPattern> existingPatterns = patternRepository.findByIsActiveTrue();
            boolean patternExists = existingPatterns.stream()
                    .anyMatch(p -> p.getPatternName().equals(name));

            if (!patternExists) {
                ThreatPattern pattern = new ThreatPattern(name, type, threatLevel);
                pattern.setPatternDefinition(definition);
                pattern.setDescription(description);
                pattern.setConfidenceThreshold(0.8);
                pattern.setIsActive(true);
                pattern.setAutoBlock(false);
                pattern.setTriggerCount(0L);
                pattern.setFalsePositiveCount(0L);

                patternRepository.save(pattern);
                log.info("Created default threat pattern: {}", name);
            } else {
                log.debug("Threat pattern already exists: {}", name);
            }
        } catch (Exception e) {
            log.error("Error creating threat pattern {}: {}", name, e.getMessage(), e);
        }
    }

    /**
     * Enhanced IP frequency pattern evaluation
     */
    private double evaluateIpFrequencyPattern(SecurityEvent event, Map<String, Object> patternDef) {
        try {
            Integer requestsPerMinute = (Integer) patternDef.get("requestsPerMinute");
            Integer timeWindowMinutes = (Integer) patternDef.get("timeWindowMinutes");

            if (requestsPerMinute == null || timeWindowMinutes == null) {
                return 0.0;
            }

            LocalDateTime windowStart = LocalDateTime.now().minusMinutes(timeWindowMinutes);
            List<SecurityEvent> recentEvents = eventRepository
                    .findByClientIpAndTimestampBetween(event.getClientIp(), windowStart, LocalDateTime.now());

            double actualRate = (double) recentEvents.size() / timeWindowMinutes;
            double expectedRate = (double) requestsPerMinute;

            // Calculate confidence based on how much the actual rate exceeds expected
            if (actualRate > expectedRate) {
                return Math.min(1.0, (actualRate - expectedRate) / expectedRate);
            }

            return 0.0;

        } catch (Exception e) {
            log.warn("Error evaluating IP frequency pattern: {}", e.getMessage());
            return 0.0;
        }
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

    /**
     * Enhanced temporal pattern evaluation
     */
    private double evaluateTemporalPattern(SecurityEvent event, Map<String, Object> patternDef) {
        try {
            @SuppressWarnings("unchecked")
            List<Integer> suspiciousHours = (List<Integer>) patternDef.get("suspiciousHours");
            if (suspiciousHours == null) return 0.0;

            int currentHour = event.getTimestamp().getHour();

            if (suspiciousHours.contains(currentHour)) {
                // Check if there's unusual activity during this hour
                LocalDateTime hourStart = event.getTimestamp().truncatedTo(ChronoUnit.HOURS);
                LocalDateTime hourEnd = hourStart.plusHours(1);

                List<SecurityEvent> hourlyEvents = eventRepository
                        .findByClientIpAndTimestampBetween(event.getClientIp(), hourStart, hourEnd);

                // More activity during suspicious hours = higher confidence
                return Math.min(1.0, hourlyEvents.size() / 10.0);
            }

            return 0.0;

        } catch (Exception e) {
            log.warn("Error evaluating temporal pattern: {}", e.getMessage());
            return 0.0;
        }
    }

    /**
     * Analyze high frequency sources
     */
    private void analyzeHighFrequencySources(LocalDateTime since) {
        try {
            List<Object[]> suspiciousIPs = eventRepository.findSuspiciousIPs(since, 20L);

            for (Object[] ipData : suspiciousIPs) {
                String ip = (String) ipData[0];
                Long count = (Long) ipData[1];

                // Check if alert already exists
                Optional<ThreatAlert> existingAlert = alertRepository
                        .findBySourceIpAndTargetRouteAndStatus(ip, null, "OPEN");

                if (existingAlert.isEmpty() && count > SUSPICIOUS_IP_THRESHOLD) {
                    ThreatAlert alert = new ThreatAlert("ANOMALY", "HIGH", "High Volume Attack Source");
                    alert.setDescription(String.format("IP %s generated %d rejections in the last hour", ip, count));
                    alert.setSourceIp(ip);
                    alert.setThreatScore(Math.min(1.0, count / (double) SUSPICIOUS_IP_THRESHOLD));
                    alert.setConfidence(0.85);

                    alertRepository.save(alert);
                    log.warn("Created high frequency source alert for IP: {} ({} events)", ip, count);
                }
            }

        } catch (Exception e) {
            log.error("Error analyzing high frequency sources: {}", e.getMessage(), e);
        }
    }

    /**
     * Analyze geographic anomalies
     */
    private void analyzeGeographicAnomalies(LocalDateTime since) {
        try {
            // This would require IP geolocation data
            // For now, analyze unusual IP patterns
            List<SecurityEvent> recentEvents = eventRepository.findByTimestampBetween(since, LocalDateTime.now());

            Map<String, Long> countryCount = new HashMap<>();
            Map<String, Set<String>> countryIPs = new HashMap<>();

            for (SecurityEvent event : recentEvents) {
                String ip = event.getClientIp();
                if (ip != null) {
                    // Simplified: use IP prefix as "country" indicator
                    String prefix = ip.substring(0, Math.min(ip.length(), ip.indexOf('.', ip.indexOf('.') + 1)));
                    countryCount.merge(prefix, 1L, Long::sum);
                    countryIPs.computeIfAbsent(prefix, k -> new HashSet<>()).add(ip);
                }
            }

            // Look for countries with unusual activity
            double avgActivity = countryCount.values().stream().mapToLong(Long::longValue).average().orElse(0.0);

            for (Map.Entry<String, Long> entry : countryCount.entrySet()) {
                if (entry.getValue() > avgActivity * 3) { // 3x average activity
                    log.info("Geographic anomaly detected: {} prefix with {} events (avg: {})",
                            entry.getKey(), entry.getValue(), avgActivity);
                }
            }

        } catch (Exception e) {
            log.error("Error analyzing geographic anomalies: {}", e.getMessage(), e);
        }
    }

    /**
     * Analyze temporal patterns
     */
    private void analyzeTemporalPatterns(LocalDateTime since) {
        try {
            List<Object[]> hourlyData = eventRepository.getHourlyEventCounts(since);

            // Calculate average requests per hour
            double avgHourly = hourlyData.stream()
                    .mapToLong(row -> (Long) row[1])
                    .average()
                    .orElse(0.0);

            // Look for hours with unusual activity
            for (Object[] hourData : hourlyData) {
                Long count = (Long) hourData[1];
                if (count > avgHourly * 2) { // 2x average
                    log.info("Temporal anomaly detected: {} events in hour (avg: {})", count, avgHourly);

                    // Could create alert for unusual temporal patterns
                    // Implementation depends on specific requirements
                }
            }

        } catch (Exception e) {
            log.error("Error analyzing temporal patterns: {}", e.getMessage(), e);
        }
    }

    /**
     * Update threat scores based on recent activity
     */
    private void updateThreatScores() {
        try {
            LocalDateTime since = LocalDateTime.now().minusHours(24);
            List<ThreatAlert> openAlerts = alertRepository.findByStatusOrderByCreatedAtDesc("OPEN");

            for (ThreatAlert alert : openAlerts) {
                if (alert.getSourceIp() != null) {
                    // Count recent events from this IP
                    List<SecurityEvent> recentEvents = eventRepository
                            .findByClientIpAndTimestampBetween(alert.getSourceIp(), since, LocalDateTime.now());

                    long rejections = recentEvents.stream()
                            .filter(e -> "REJECTION".equals(e.getEventType()))
                            .count();

                    // Update threat score based on recent activity
                    if (rejections > 50) {
                        alert.setThreatScore(Math.min(1.0, alert.getThreatScore() + 0.1));
                        alertRepository.save(alert);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error updating threat scores: {}", e.getMessage(), e);
        }
    }

    private Map<String, Object> getSuspiciousActivityStats(LocalDateTime since) {
        Map<String, Object> stats = new HashMap<>();
        List<Object[]> suspiciousIPs = eventRepository.findSuspiciousIPs(since, 10L);
        stats.put("suspiciousIPCount", suspiciousIPs.size());
        stats.put("topSuspiciousIPs", suspiciousIPs.stream().limit(5).collect(Collectors.toList()));
        return stats;
    }

    /**
     * Analyze traffic volume trends for recommendations
     */
    private void analyzeTrafficVolumeTrends(List<Map<String, Object>> recommendations, LocalDateTime since) {
        try {
            List<Object[]> hourlyData = eventRepository.getHourlyEventCounts(since);

            if (hourlyData.size() >= 2) {
                // Compare recent hours
                Long recentHour = (Long) hourlyData.get(hourlyData.size() - 1)[1];
                Long previousHour = (Long) hourlyData.get(hourlyData.size() - 2)[1];

                if (recentHour > previousHour * 1.5) { // 50% increase
                    Map<String, Object> recommendation = new HashMap<>();
                    recommendation.put("type", "TRAFFIC_SPIKE");
                    recommendation.put("priority", "MEDIUM");
                    recommendation.put("title", "Unusual traffic spike detected");
                    recommendation.put("description", String.format("Traffic increased from %d to %d requests per hour", previousHour, recentHour));
                    recommendations.add(recommendation);
                }
            }

        } catch (Exception e) {
            log.warn("Error analyzing traffic volume trends: {}", e.getMessage());
        }
    }

    /**
     * Analyze pattern effectiveness for recommendations
     */
    private void analyzePatternEffectiveness(List<Map<String, Object>> recommendations) {
        try {
            LocalDateTime since = LocalDateTime.now().minusDays(7);
            List<ThreatPattern> patterns = patternRepository.findRecentlyTriggeredPatterns(since);

            // Find patterns with high false positive rates
            for (ThreatPattern pattern : patterns) {
                if (pattern.getTriggerCount() > 0) {
                    double falsePositiveRate = (double) pattern.getFalsePositiveCount() / pattern.getTriggerCount();

                    if (falsePositiveRate > 0.3) { // 30% false positive rate
                        Map<String, Object> recommendation = new HashMap<>();
                        recommendation.put("type", "PATTERN_TUNING");
                        recommendation.put("priority", "LOW");
                        recommendation.put("title", "Pattern needs tuning: " + pattern.getPatternName());
                        recommendation.put("description", String.format("Pattern has %.1f%% false positive rate", falsePositiveRate * 100));
                        recommendations.add(recommendation);
                    }
                }
            }

        } catch (Exception e) {
            log.warn("Error analyzing pattern effectiveness: {}", e.getMessage());
        }
    }

    /**
     * Analyze response time anomalies for recommendations
     */
    private void analyzeResponseTimeAnomalies(List<Map<String, Object>> recommendations, LocalDateTime since) {
        try {
            List<Object[]> avgResponseTimes = eventRepository.getAverageResponseTimeByRoute(since);

            for (Object[] routeData : avgResponseTimes) {
                String routeId = (String) routeData[0];
                Double avgResponseTime = (Double) routeData[1];

                if (avgResponseTime != null && avgResponseTime > 5000) { // 5 seconds
                    Map<String, Object> recommendation = new HashMap<>();
                    recommendation.put("type", "PERFORMANCE");
                    recommendation.put("priority", "MEDIUM");
                    recommendation.put("title", "Slow response time detected");
                    recommendation.put("description", String.format("Route %s has average response time of %.1f ms", routeId, avgResponseTime));
                    recommendations.add(recommendation);
                }
            }

        } catch (Exception e) {
            log.warn("Error analyzing response time anomalies: {}", e.getMessage());
        }
    }
}