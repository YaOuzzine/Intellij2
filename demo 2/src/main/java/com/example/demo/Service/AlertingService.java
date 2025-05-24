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
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class AlertingService {

    private static final Logger log = LoggerFactory.getLogger(AlertingService.class);

    private final ThreatAlertRepository alertRepository;
    private final SecurityEventRepository eventRepository;
    private final ThreatPatternRepository patternRepository;
    private final ObjectMapper objectMapper;

    // Alert thresholds and configuration
    private static final int HIGH_FREQUENCY_THRESHOLD = 100; // requests per minute
    private static final int CRITICAL_REJECTION_THRESHOLD = 50; // rejections per minute
    private static final double ANOMALY_DETECTION_THRESHOLD = 0.8;
    private static final int MAX_ALERTS_PER_HOUR = 50; // Rate limiting for alerts
    private static final int ALERT_SUPPRESSION_MINUTES = 15; // Suppress duplicate alerts

    // In-memory tracking for real-time alerting
    private final Map<String, AlertTracker> alertTrackers = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastAlertTimes = new ConcurrentHashMap<>();
    private final AtomicInteger alertsThisHour = new AtomicInteger(0);
    private LocalDateTime currentHourStart = LocalDateTime.now().withMinute(0).withSecond(0);

    // Alert subscribers (for future webhook/notification integrations)
    private final List<AlertSubscriber> alertSubscribers = new ArrayList<>();

    @Autowired
    public AlertingService(ThreatAlertRepository alertRepository,
                           SecurityEventRepository eventRepository,
                           ThreatPatternRepository patternRepository) {
        this.alertRepository = alertRepository;
        this.eventRepository = eventRepository;
        this.patternRepository = patternRepository;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void initializeAlertingService() {
        log.info("Initializing Alerting Service...");

        // Initialize default alert subscribers
        initializeDefaultSubscribers();

        // Load existing open alerts into memory for tracking
        loadExistingAlerts();

        log.info("Alerting Service initialized successfully");
    }

    /**
     * Process a security event and trigger alerts if necessary
     */
    @Async
    @Transactional
    public CompletableFuture<List<ThreatAlert>> processSecurityEvent(SecurityEvent event) {
        List<ThreatAlert> triggeredAlerts = new ArrayList<>();

        try {
            log.debug("Processing security event for alerting: type={}, clientIp={}, routeId={}",
                    event.getEventType(), event.getClientIp(), event.getRouteId());

            // Check rate limiting
            if (!canCreateAlert()) {
                log.warn("Alert rate limit exceeded, skipping alert creation");
                return CompletableFuture.completedFuture(triggeredAlerts);
            }

            // Real-time anomaly detection
            ThreatAlert anomalyAlert = detectRealTimeAnomaly(event);
            if (anomalyAlert != null) {
                triggeredAlerts.add(anomalyAlert);
            }

            // High-frequency attack detection
            ThreatAlert frequencyAlert = detectHighFrequencyAttack(event);
            if (frequencyAlert != null) {
                triggeredAlerts.add(frequencyAlert);
            }

            // Geographic anomaly detection
            ThreatAlert geoAlert = detectGeographicAnomaly(event);
            if (geoAlert != null) {
                triggeredAlerts.add(geoAlert);
            }

            // Critical event detection
            ThreatAlert criticalAlert = detectCriticalEvent(event);
            if (criticalAlert != null) {
                triggeredAlerts.add(criticalAlert);
            }

            // Save and notify for all triggered alerts
            for (ThreatAlert alert : triggeredAlerts) {
                alert = alertRepository.save(alert);
                notifySubscribers(alert);
                updateAlertTracker(alert);
                log.info("Alert triggered: {} for IP: {}", alert.getTitle(), event.getClientIp());
            }

        } catch (Exception e) {
            log.error("Error processing security event for alerting: {}", e.getMessage(), e);
        }

        return CompletableFuture.completedFuture(triggeredAlerts);
    }

    /**
     * Get current active alerts
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getActiveAlerts() {
        List<ThreatAlert> activeAlerts = alertRepository.findByStatusOrderByCreatedAtDesc("OPEN");

        return activeAlerts.stream().map(alert -> {
            Map<String, Object> alertInfo = new HashMap<>();
            alertInfo.put("id", alert.getId());
            alertInfo.put("title", alert.getTitle());
            alertInfo.put("severity", alert.getSeverity());
            alertInfo.put("sourceIp", alert.getSourceIp());
            alertInfo.put("targetRoute", alert.getTargetRoute());
            alertInfo.put("threatScore", alert.getThreatScore());
            alertInfo.put("confidence", alert.getConfidence());
            alertInfo.put("eventCount", alert.getEventCount());
            alertInfo.put("firstSeen", alert.getFirstSeen());
            alertInfo.put("lastSeen", alert.getLastSeen());
            alertInfo.put("description", alert.getDescription());
            alertInfo.put("status", alert.getStatus());

            // Add time since first seen
            long minutesSinceFirst = java.time.Duration.between(alert.getFirstSeen(), LocalDateTime.now()).toMinutes();
            alertInfo.put("minutesSinceFirst", minutesSinceFirst);

            return alertInfo;
        }).collect(Collectors.toList());
    }

    /**
     * Get alert statistics and metrics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAlertStatistics() {
        Map<String, Object> stats = new HashMap<>();

        try {
            LocalDateTime since = LocalDateTime.now().minusHours(24);

            // Count alerts by severity in last 24 hours
            List<Object[]> severityDistribution = alertRepository.getAlertSeverityDistribution(since);
            Map<String, Long> severityMap = severityDistribution.stream()
                    .collect(Collectors.toMap(
                            arr -> (String) arr[0],
                            arr -> (Long) arr[1]
                    ));

            // Get critical alerts count
            Long criticalAlertsCount = alertRepository.countOpenAlerts();
            List<ThreatAlert> criticalAlerts = alertRepository.findActiveCriticalAlerts();

            // Calculate alert resolution metrics
            List<ThreatAlert> recentAlerts = alertRepository.findRecentAlerts(since);
            long resolvedAlerts = recentAlerts.stream()
                    .filter(a -> "RESOLVED".equals(a.getStatus()))
                    .count();

            double resolutionRate = recentAlerts.isEmpty() ? 0.0 :
                    (double) resolvedAlerts / recentAlerts.size() * 100;

            // Calculate average response time
            double avgResponseTime = recentAlerts.stream()
                    .filter(a -> a.getUpdatedAt() != null && "RESOLVED".equals(a.getStatus()))
                    .mapToLong(a -> java.time.Duration.between(a.getCreatedAt(), a.getUpdatedAt()).toMinutes())
                    .average().orElse(0.0);

            stats.put("totalOpenAlerts", criticalAlertsCount);
            stats.put("criticalAlertsCount", criticalAlerts.size());
            stats.put("severityDistribution", severityMap);
            stats.put("alertsLast24h", recentAlerts.size());
            stats.put("resolutionRate", resolutionRate);
            stats.put("avgResponseTimeMinutes", avgResponseTime);
            stats.put("alertsThisHour", alertsThisHour.get());
            stats.put("generatedAt", LocalDateTime.now());

            // Top alert sources
            Map<String, Long> topSources = recentAlerts.stream()
                    .filter(a -> a.getSourceIp() != null)
                    .collect(Collectors.groupingBy(ThreatAlert::getSourceIp, Collectors.counting()));

            List<Map<String, Object>> topSourcesList = topSources.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(5)
                    .map(entry -> {
                        Map<String, Object> sourceInfo = new HashMap<>();
                        sourceInfo.put("ip", entry.getKey());
                        sourceInfo.put("alertCount", entry.getValue());
                        return sourceInfo;
                    })
                    .collect(Collectors.toList());

            stats.put("topAlertSources", topSourcesList);

        } catch (Exception e) {
            log.error("Error generating alert statistics: {}", e.getMessage(), e);
        }

        return stats;
    }

    /**
     * Update alert status (resolve, investigate, etc.)
     */
    @Transactional
    public ThreatAlert updateAlertStatus(Long alertId, String status, String notes) {
        try {
            ThreatAlert alert = alertRepository.findById(alertId)
                    .orElseThrow(() -> new RuntimeException("Alert not found with ID: " + alertId));

            String oldStatus = alert.getStatus();
            alert.setStatus(status);

            if (notes != null && !notes.trim().isEmpty()) {
                alert.setResolutionNotes(notes);
            }

            alert.setUpdatedAt(LocalDateTime.now());

            alert = alertRepository.save(alert);

            log.info("Alert {} status updated from {} to {} by user", alertId, oldStatus, status);

            // Notify subscribers of status change
            notifyStatusChange(alert, oldStatus, status);

            return alert;

        } catch (Exception e) {
            log.error("Error updating alert status: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update alert status", e);
        }
    }

    /**
     * Auto-resolve alerts based on patterns and time
     */
    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    @Transactional
    public void autoResolveAlerts() {
        try {
            log.debug("Running auto-resolution check for alerts...");

            LocalDateTime cutoff = LocalDateTime.now().minusHours(2); // Auto-resolve after 2 hours of inactivity
            List<ThreatAlert> staleAlerts = alertRepository.findByStatusOrderByCreatedAtDesc("OPEN")
                    .stream()
                    .filter(alert -> alert.getLastSeen().isBefore(cutoff))
                    .collect(Collectors.toList());

            for (ThreatAlert alert : staleAlerts) {
                // Check if the threat is still active
                if (!isThreatStillActive(alert)) {
                    alert.setStatus("RESOLVED");
                    alert.setAutoResolved(true);
                    alert.setResolutionNotes("Auto-resolved: No recent activity detected");
                    alert.setUpdatedAt(LocalDateTime.now());

                    alertRepository.save(alert);
                    log.info("Auto-resolved stale alert: {} for IP: {}", alert.getTitle(), alert.getSourceIp());
                }
            }

            // Clean up alert trackers
            cleanupAlertTrackers();

        } catch (Exception e) {
            log.error("Error in auto-resolution process: {}", e.getMessage(), e);
        }
    }

    /**
     * Generate alert summary report
     */
    @Transactional(readOnly = true)
    public Map<String, Object> generateAlertReport(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> report = new HashMap<>();

        try {
            List<ThreatAlert> alerts = alertRepository.findRecentAlerts(startTime);

            // Filter by end time
            alerts = alerts.stream()
                    .filter(a -> a.getCreatedAt().isBefore(endTime))
                    .collect(Collectors.toList());

            // Basic metrics
            report.put("totalAlerts", alerts.size());
            report.put("reportPeriod", startTime + " to " + endTime);

            // Severity breakdown
            Map<String, Long> severityBreakdown = alerts.stream()
                    .collect(Collectors.groupingBy(ThreatAlert::getSeverity, Collectors.counting()));
            report.put("severityBreakdown", severityBreakdown);

            // Status breakdown
            Map<String, Long> statusBreakdown = alerts.stream()
                    .collect(Collectors.groupingBy(ThreatAlert::getStatus, Collectors.counting()));
            report.put("statusBreakdown", statusBreakdown);

            // Top alert types
            Map<String, Long> alertTypes = alerts.stream()
                    .collect(Collectors.groupingBy(ThreatAlert::getAlertType, Collectors.counting()));
            report.put("alertTypes", alertTypes);

            // Resolution metrics
            List<ThreatAlert> resolvedAlerts = alerts.stream()
                    .filter(a -> "RESOLVED".equals(a.getStatus()) && a.getUpdatedAt() != null)
                    .collect(Collectors.toList());

            if (!resolvedAlerts.isEmpty()) {
                double avgResolutionTime = resolvedAlerts.stream()
                        .mapToLong(a -> java.time.Duration.between(a.getCreatedAt(), a.getUpdatedAt()).toMinutes())
                        .average().orElse(0.0);

                report.put("averageResolutionTimeMinutes", avgResolutionTime);
                report.put("resolutionRate", (double) resolvedAlerts.size() / alerts.size() * 100);
            }

            // Trending analysis
            Map<String, Object> trends = analyzeTrends(alerts);
            report.put("trends", trends);

            report.put("generatedAt", LocalDateTime.now());

        } catch (Exception e) {
            log.error("Error generating alert report: {}", e.getMessage(), e);
        }

        return report;
    }

    /**
     * Subscribe to alert notifications
     */
    public void subscribeToAlerts(AlertSubscriber subscriber) {
        alertSubscribers.add(subscriber);
        log.info("New alert subscriber added: {}", subscriber.getName());
    }

    /**
     * Unsubscribe from alert notifications
     */
    public void unsubscribeFromAlerts(AlertSubscriber subscriber) {
        alertSubscribers.remove(subscriber);
        log.info("Alert subscriber removed: {}", subscriber.getName());
    }

    // Private helper methods

    private void initializeDefaultSubscribers() {
        // Console logger subscriber
        AlertSubscriber consoleSubscriber = new AlertSubscriber() {
            @Override
            public void onAlertTriggered(ThreatAlert alert) {
                log.warn("ALERT TRIGGERED: [{}] {} - {} (Threat Score: {:.2f})",
                        alert.getSeverity(), alert.getTitle(), alert.getSourceIp(), alert.getThreatScore());
            }

            @Override
            public void onAlertStatusChanged(ThreatAlert alert, String oldStatus, String newStatus) {
                log.info("ALERT STATUS CHANGED: {} -> {} for alert: {}",
                        oldStatus, newStatus, alert.getTitle());
            }

            @Override
            public String getName() {
                return "ConsoleLogger";
            }
        };

        alertSubscribers.add(consoleSubscriber);
    }

    private void loadExistingAlerts() {
        try {
            List<ThreatAlert> openAlerts = alertRepository.findByStatusOrderByCreatedAtDesc("OPEN");
            for (ThreatAlert alert : openAlerts) {
                String key = generateAlertKey(alert.getSourceIp(), alert.getAlertType());
                alertTrackers.put(key, new AlertTracker(alert));
            }
            log.info("Loaded {} existing open alerts into memory", openAlerts.size());
        } catch (Exception e) {
            log.error("Error loading existing alerts: {}", e.getMessage());
        }
    }

    private boolean canCreateAlert() {
        // Check hourly rate limit
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime hourStart = now.withMinute(0).withSecond(0);

        if (!hourStart.equals(currentHourStart)) {
            // New hour, reset counter
            currentHourStart = hourStart;
            alertsThisHour.set(0);
        }

        return alertsThisHour.get() < MAX_ALERTS_PER_HOUR;
    }

    private ThreatAlert detectRealTimeAnomaly(SecurityEvent event) {
        try {
            String clientIp = event.getClientIp();
            String alertKey = generateAlertKey(clientIp, "ANOMALY");

            // Check for suppression
            if (isAlertSuppressed(alertKey)) {
                return null;
            }

            // Get recent events for this IP
            LocalDateTime since = LocalDateTime.now().minusMinutes(10);
            List<SecurityEvent> recentEvents = eventRepository
                    .findByClientIpAndTimestampBetween(clientIp, since, LocalDateTime.now());

            if (recentEvents.size() < 5) return null; // Need minimum events for anomaly detection

            // Check for unusual patterns
            boolean isAnomalous = false;
            String anomalyDescription = "";

            // Check frequency anomaly
            if (recentEvents.size() > HIGH_FREQUENCY_THRESHOLD / 6) { // 10 minutes = 1/6 hour
                isAnomalous = true;
                anomalyDescription += "High frequency activity: " + recentEvents.size() + " events in 10 minutes. ";
            }

            // Check for unusual rejection rate
            long rejections = recentEvents.stream()
                    .filter(e -> "REJECTION".equals(e.getEventType()))
                    .count();

            if (rejections > recentEvents.size() * 0.7) { // 70% rejection rate
                isAnomalous = true;
                anomalyDescription += "High rejection rate: " + (rejections * 100 / recentEvents.size()) + "%. ";
            }

            // Check for new/unusual paths
            Set<String> paths = recentEvents.stream()
                    .map(SecurityEvent::getRequestPath)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            if (paths.size() > 20) { // Many different paths in short time
                isAnomalous = true;
                anomalyDescription += "Path scanning detected: " + paths.size() + " unique paths. ";
            }

            if (isAnomalous) {
                ThreatAlert alert = new ThreatAlert("ANOMALY", "HIGH", "Real-time Anomaly Detected");
                alert.setDescription(anomalyDescription.trim());
                alert.setSourceIp(clientIp);
                alert.setTargetRoute(event.getRouteId());
                alert.setThreatScore(0.8);
                alert.setConfidence(0.85);
                alert.setEventCount((long) recentEvents.size());

                return alert;
            }

        } catch (Exception e) {
            log.warn("Error in real-time anomaly detection: {}", e.getMessage());
        }

        return null;
    }

    private ThreatAlert detectHighFrequencyAttack(SecurityEvent event) {
        try {
            String clientIp = event.getClientIp();
            String alertKey = generateAlertKey(clientIp, "HIGH_FREQUENCY");

            // Check for suppression
            if (isAlertSuppressed(alertKey)) {
                return null;
            }

            // Check request frequency in last minute
            LocalDateTime since = LocalDateTime.now().minusMinutes(1);
            List<SecurityEvent> lastMinuteEvents = eventRepository
                    .findByClientIpAndTimestampBetween(clientIp, since, LocalDateTime.now());

            if (lastMinuteEvents.size() > HIGH_FREQUENCY_THRESHOLD / 60) { // Per minute threshold
                // Check if this is a sustained attack (not just a burst)
                LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
                List<SecurityEvent> fiveMinuteEvents = eventRepository
                        .findByClientIpAndTimestampBetween(clientIp, fiveMinutesAgo, LocalDateTime.now());

                if (fiveMinuteEvents.size() > HIGH_FREQUENCY_THRESHOLD / 12) { // Sustained over 5 minutes
                    ThreatAlert alert = new ThreatAlert("PATTERN_MATCH", "CRITICAL", "High-Frequency Attack Detected");
                    alert.setDescription(String.format("IP %s generated %d requests in the last minute and %d in the last 5 minutes",
                            clientIp, lastMinuteEvents.size(), fiveMinuteEvents.size()));
                    alert.setSourceIp(clientIp);
                    alert.setTargetRoute(event.getRouteId());
                    alert.setThreatScore(0.95);
                    alert.setConfidence(0.9);
                    alert.setEventCount((long) fiveMinuteEvents.size());

                    return alert;
                }
            }

        } catch (Exception e) {
            log.warn("Error in high-frequency attack detection: {}", e.getMessage());
        }

        return null;
    }

    private ThreatAlert detectGeographicAnomaly(SecurityEvent event) {
        try {
            // Simplified geographic detection based on IP patterns
            String clientIp = event.getClientIp();
            String ipPrefix = clientIp.substring(0, Math.min(clientIp.length(),
                    clientIp.indexOf('.', clientIp.indexOf('.') + 1)));

            String alertKey = generateAlertKey(ipPrefix, "GEO_ANOMALY");

            if (isAlertSuppressed(alertKey)) {
                return null;
            }

            // Check for coordinated attack from same IP range
            LocalDateTime since = LocalDateTime.now().minusMinutes(15);
            List<SecurityEvent> recentEvents = eventRepository.findByTimestampBetween(since, LocalDateTime.now());

            long sameRangeEvents = recentEvents.stream()
                    .filter(e -> e.getClientIp().startsWith(ipPrefix))
                    .count();

            Set<String> uniqueIpsInRange = recentEvents.stream()
                    .filter(e -> e.getClientIp().startsWith(ipPrefix))
                    .map(SecurityEvent::getClientIp)
                    .collect(Collectors.toSet());

            // If many IPs from same range are attacking
            if (uniqueIpsInRange.size() > 5 && sameRangeEvents > 50) {
                ThreatAlert alert = new ThreatAlert("ANOMALY", "HIGH", "Geographic Attack Cluster Detected");
                alert.setDescription(String.format("Coordinated attack from IP range %s.x.x: %d unique IPs, %d total events",
                        ipPrefix, uniqueIpsInRange.size(), sameRangeEvents));
                alert.setSourceIp(ipPrefix + ".x.x");
                alert.setTargetRoute(event.getRouteId());
                alert.setThreatScore(0.75);
                alert.setConfidence(0.8);
                alert.setEventCount(sameRangeEvents);

                return alert;
            }

        } catch (Exception e) {
            log.warn("Error in geographic anomaly detection: {}", e.getMessage());
        }

        return null;
    }

    private ThreatAlert detectCriticalEvent(SecurityEvent event) {
        // Detect immediately critical events that need urgent attention
        if ("CRITICAL".equals(event.getThreatLevel()) && "REJECTION".equals(event.getEventType())) {
            String alertKey = generateAlertKey(event.getClientIp(), "CRITICAL_EVENT");

            if (!isAlertSuppressed(alertKey)) {
                ThreatAlert alert = new ThreatAlert("THRESHOLD_BREACH", "CRITICAL", "Critical Security Event");
                alert.setDescription("Critical threat level event detected: " + event.getRejectionReason());
                alert.setSourceIp(event.getClientIp());
                alert.setTargetRoute(event.getRouteId());
                alert.setThreatScore(1.0);
                alert.setConfidence(0.95);
                alert.setEventCount(1L);

                return alert;
            }
        }

        return null;
    }

    private boolean isAlertSuppressed(String alertKey) {
        LocalDateTime lastAlert = lastAlertTimes.get(alertKey);
        if (lastAlert != null) {
            LocalDateTime suppressionCutoff = LocalDateTime.now().minusMinutes(ALERT_SUPPRESSION_MINUTES);
            return lastAlert.isAfter(suppressionCutoff);
        }
        return false;
    }

    private String generateAlertKey(String identifier, String alertType) {
        return identifier + ":" + alertType;
    }

    private void notifySubscribers(ThreatAlert alert) {
        for (AlertSubscriber subscriber : alertSubscribers) {
            try {
                subscriber.onAlertTriggered(alert);
            } catch (Exception e) {
                log.error("Error notifying subscriber {}: {}", subscriber.getName(), e.getMessage());
            }
        }
    }

    private void notifyStatusChange(ThreatAlert alert, String oldStatus, String newStatus) {
        for (AlertSubscriber subscriber : alertSubscribers) {
            try {
                subscriber.onAlertStatusChanged(alert, oldStatus, newStatus);
            } catch (Exception e) {
                log.error("Error notifying subscriber of status change {}: {}", subscriber.getName(), e.getMessage());
            }
        }
    }

    private void updateAlertTracker(ThreatAlert alert) {
        String key = generateAlertKey(alert.getSourceIp(), alert.getAlertType());
        alertTrackers.put(key, new AlertTracker(alert));
        lastAlertTimes.put(key, LocalDateTime.now());
        alertsThisHour.incrementAndGet();
    }

    private boolean isThreatStillActive(ThreatAlert alert) {
        try {
            // Check if there are recent events from the same source
            LocalDateTime since = LocalDateTime.now().minusMinutes(30);
            List<SecurityEvent> recentEvents = eventRepository
                    .findByClientIpAndTimestampBetween(alert.getSourceIp(), since, LocalDateTime.now());

            // If no recent activity, threat is likely resolved
            return !recentEvents.isEmpty();

        } catch (Exception e) {
            log.warn("Error checking if threat is still active: {}", e.getMessage());
            return true; // Default to keeping alert open if we can't determine
        }
    }

    private void cleanupAlertTrackers() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);

        alertTrackers.entrySet().removeIf(entry ->
                entry.getValue().getLastUpdate().isBefore(cutoff));

        lastAlertTimes.entrySet().removeIf(entry ->
                entry.getValue().isBefore(cutoff));
    }

    private Map<String, Object> analyzeTrends(List<ThreatAlert> alerts) {
        Map<String, Object> trends = new HashMap<>();

        try {
            if (alerts.isEmpty()) {
                trends.put("trend", "NO_DATA");
                return trends;
            }

            // Sort alerts by creation time
            alerts.sort(Comparator.comparing(ThreatAlert::getCreatedAt));

            // Analyze alert frequency trend
            int midPoint = alerts.size() / 2;
            List<ThreatAlert> firstHalf = alerts.subList(0, midPoint);
            List<ThreatAlert> secondHalf = alerts.subList(midPoint, alerts.size());

            long firstHalfCritical = firstHalf.stream()
                    .filter(a -> "CRITICAL".equals(a.getSeverity()))
                    .count();

            long secondHalfCritical = secondHalf.stream()
                    .filter(a -> "CRITICAL".equals(a.getSeverity()))
                    .count();

            if (secondHalfCritical > firstHalfCritical * 1.5) {
                trends.put("severityTrend", "INCREASING");
            } else if (secondHalfCritical < firstHalfCritical * 0.5) {
                trends.put("severityTrend", "DECREASING");
            } else {
                trends.put("severityTrend", "STABLE");
            }

            // Analyze most common alert types
            Map<String, Long> typeFrequency = alerts.stream()
                    .collect(Collectors.groupingBy(ThreatAlert::getAlertType, Collectors.counting()));

            String mostCommonType = typeFrequency.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("UNKNOWN");

            trends.put("mostCommonAlertType", mostCommonType);
            trends.put("typeDistribution", typeFrequency);

            // Calculate alert resolution trends
            long totalResolved = alerts.stream()
                    .filter(a -> "RESOLVED".equals(a.getStatus()))
                    .count();

            trends.put("resolutionEfficiency", alerts.isEmpty() ? 0.0 : (double) totalResolved / alerts.size() * 100);

        } catch (Exception e) {
            log.warn("Error analyzing trends: {}", e.getMessage());
            trends.put("trend", "ERROR");
        }

        return trends;
    }

    /**
     * Alert tracker for maintaining state
     */
    private static class AlertTracker {
        private final ThreatAlert alert;
        private final LocalDateTime lastUpdate;

        public AlertTracker(ThreatAlert alert) {
            this.alert = alert;
            this.lastUpdate = LocalDateTime.now();
        }

        public ThreatAlert getAlert