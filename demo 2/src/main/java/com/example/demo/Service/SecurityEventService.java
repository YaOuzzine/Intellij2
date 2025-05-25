package com.example.demo.Service;

import com.example.demo.Entity.SecurityEvent;
import com.example.demo.Repository.SecurityEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class SecurityEventService {

    private static final Logger log = LoggerFactory.getLogger(SecurityEventService.class);
    private final SecurityEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    // Constants for threat level calculation
    private static final int HIGH_THREAT_REJECTION_COUNT = 50;
    private static final int MEDIUM_THREAT_REJECTION_COUNT = 20;
    private static final int CRITICAL_RESPONSE_TIME = 5000; // 5 seconds

    @Autowired
    public SecurityEventService(SecurityEventRepository eventRepository) {
        this.eventRepository = eventRepository;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Record a security event asynchronously for better performance
     */
    @Autowired
    private ThreatAnalysisService threatAnalysisService;

    @Autowired
    private AlertingService alertingService;

    @Autowired
    private GeolocationService geolocationService;
    @Async
    @Transactional
    public CompletableFuture<SecurityEvent> recordEventAsync(SecurityEvent event) {
        try {
            log.debug("Recording security event: type={}, routeId={}, clientIp={}",
                    event.getEventType(), event.getRouteId(), event.getClientIp());

            // Calculate threat level based on event characteristics
            event.setThreatLevel(calculateThreatLevel(event));

            // Add geolocation data if IP is available
            if (event.getClientIp() != null) {
                try {
                    GeolocationService.GeolocationData location = geolocationService.getLocation(event.getClientIp());
                    event.setGeoLocation(objectMapper.writeValueAsString(Map.of(
                            "country", location.getCountry(),
                            "city", location.getCity(),
                            "region", location.getRegion(),
                            "countryCode", location.getCountryCode()
                    )));
                } catch (Exception e) {
                    log.warn("Failed to get geolocation for IP {}: {}", event.getClientIp(), e.getMessage());
                }
            }

            SecurityEvent savedEvent = eventRepository.save(event);
            log.trace("Security event saved with ID: {}", savedEvent.getId());

            // CRITICAL ADDITION: Trigger automatic threat analysis
            try {
                if (threatAnalysisService != null) {
                    threatAnalysisService.analyzeEvent(savedEvent);
                    log.debug("Triggered threat analysis for event ID: {}", savedEvent.getId());
                }

                if (alertingService != null) {
                    alertingService.processSecurityEvent(savedEvent);
                    log.debug("Triggered alerting analysis for event ID: {}", savedEvent.getId());
                }
            } catch (Exception e) {
                log.error("Error triggering threat analysis for event {}: {}", savedEvent.getId(), e.getMessage(), e);
                // Don't fail the event recording if threat analysis fails
            }

            return CompletableFuture.completedFuture(savedEvent);
        } catch (Exception e) {
            log.error("Error recording security event: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Record a security event synchronously
     */

    @Transactional
    public SecurityEvent recordEvent(SecurityEvent event) {
        try {
            event.setThreatLevel(calculateThreatLevel(event));
            SecurityEvent savedEvent = eventRepository.save(event);
            log.debug("Recorded security event: ID={}, type={}, threatLevel={}",
                    savedEvent.getId(), savedEvent.getEventType(), savedEvent.getThreatLevel());
            return savedEvent;
        } catch (Exception e) {
            log.error("Error recording security event synchronously: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to record security event", e);
        }
    }

    /**
     * Record a simple request event
     */
    public void recordRequest(String routeId, String clientIp, String requestPath, String method) {
        SecurityEvent event = new SecurityEvent("REQUEST", routeId, clientIp);
        event.setRequestPath(requestPath);
        event.setRequestMethod(method);
        recordEventAsync(event);
    }

    /**
     * Record a rejection event with detailed information
     */
    public void recordRejection(String routeId, String clientIp, String requestPath,
                                String rejectionReason, Integer responseStatus, String userAgent) {
        SecurityEvent event = new SecurityEvent("REJECTION", routeId, clientIp);
        event.setRequestPath(requestPath);
        event.setRejectionReason(rejectionReason);
        event.setResponseStatus(responseStatus);
        event.setUserAgent(userAgent);
        recordEventAsync(event);
    }

    /**
     * Record response time for completed requests
     */
    public void recordResponseTime(String routeId, String clientIp, String requestPath,
                                   Integer responseTimeMs, Integer responseStatus) {
        SecurityEvent event = new SecurityEvent("REQUEST", routeId, clientIp);
        event.setRequestPath(requestPath);
        event.setResponseTimeMs(responseTimeMs);
        event.setResponseStatus(responseStatus);
        recordEventAsync(event);
    }

    /**
     * Get events for a specific time range
     */
    @Transactional(readOnly = true)
    public List<SecurityEvent> getEventsInTimeRange(LocalDateTime start, LocalDateTime end) {
        return eventRepository.findByTimestampBetween(start, end);
    }

    /**
     * Get events for a specific route
     */
    @Transactional(readOnly = true)
    public List<SecurityEvent> getRouteEvents(String routeId, LocalDateTime start, LocalDateTime end) {
        return eventRepository.findByRouteIdAndTimestampBetween(routeId, start, end);
    }

    /**
     * Get aggregated metrics for analytics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAggregatedMetrics(String routeId, LocalDateTime start, LocalDateTime end) {
        Map<String, Object> metrics = new HashMap<>();

        try {
            List<SecurityEvent> events = routeId != null ?
                    eventRepository.findByRouteIdAndTimestampBetween(routeId, start, end) :
                    eventRepository.findByTimestampBetween(start, end);

            long totalRequests = events.stream()
                    .filter(e -> "REQUEST".equals(e.getEventType()))
                    .count();

            long totalRejections = events.stream()
                    .filter(e -> "REJECTION".equals(e.getEventType()))
                    .count();

            double avgResponseTime = events.stream()
                    .filter(e -> e.getResponseTimeMs() != null)
                    .mapToInt(SecurityEvent::getResponseTimeMs)
                    .average()
                    .orElse(0.0);

            long uniqueIPs = events.stream()
                    .map(SecurityEvent::getClientIp)
                    .distinct()
                    .count();

            Map<String, Long> threatLevelCounts = events.stream()
                    .collect(Collectors.groupingBy(
                            SecurityEvent::getThreatLevel,
                            Collectors.counting()
                    ));

            metrics.put("totalRequests", totalRequests);
            metrics.put("totalRejections", totalRejections);
            metrics.put("acceptanceRate", totalRequests > 0 ?
                    ((double)(totalRequests - totalRejections) / totalRequests) * 100 : 100.0);
            metrics.put("avgResponseTime", avgResponseTime);
            metrics.put("uniqueIPs", uniqueIPs);
            metrics.put("threatLevelDistribution", threatLevelCounts);

            log.debug("Generated aggregated metrics for routeId={}: {} requests, {} rejections, {} unique IPs",
                    routeId, totalRequests, totalRejections, uniqueIPs);

        } catch (Exception e) {
            log.error("Error generating aggregated metrics: {}", e.getMessage(), e);
            // Return empty metrics on error
            metrics.put("totalRequests", 0L);
            metrics.put("totalRejections", 0L);
            metrics.put("acceptanceRate", 100.0);
            metrics.put("avgResponseTime", 0.0);
            metrics.put("uniqueIPs", 0L);
            metrics.put("threatLevelDistribution", new HashMap<>());
        }

        return metrics;
    }

    /**
     * Get time series data for charts
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTimeSeriesData(LocalDateTime start, LocalDateTime end, String granularity) {
        List<Map<String, Object>> timeSeriesData = new ArrayList<>();

        try {
            List<Object[]> data;
            if ("minute".equalsIgnoreCase(granularity)) {
                data = eventRepository.getMinutelyEventCounts(start);
            } else {
                data = eventRepository.getHourlyEventCounts(start);
            }

            for (Object[] row : data) {
                Map<String, Object> point = new HashMap<>();
                point.put("timestamp", row[0]);
                point.put("count", row[1]);
                timeSeriesData.add(point);
            }

            log.debug("Generated {} time series data points with {} granularity",
                    timeSeriesData.size(), granularity);

        } catch (Exception e) {
            log.error("Error generating time series data: {}", e.getMessage(), e);
        }

        return timeSeriesData;
    }

    /**
     * Get suspicious activity report
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getSuspiciousActivityReport(LocalDateTime since) {
        Map<String, Object> report = new HashMap<>();

        try {
            // Find IPs with high rejection rates
            List<Object[]> suspiciousIPs = eventRepository.findSuspiciousIPs(since, 10L);

            // Get top attacking IPs
            List<Object[]> topAttackers = eventRepository.findTopAttackingIPs(since);

            // Get threat level distribution
            List<Object[]> threatDistribution = eventRepository.getThreatLevelDistribution(since);

            report.put("suspiciousIPs", suspiciousIPs);
            report.put("topAttackers", topAttackers);
            report.put("threatDistribution", threatDistribution);
            report.put("reportGeneratedAt", LocalDateTime.now());

            log.info("Generated suspicious activity report: {} suspicious IPs, {} top attackers",
                    suspiciousIPs.size(), topAttackers.size());

        } catch (Exception e) {
            log.error("Error generating suspicious activity report: {}", e.getMessage(), e);
        }

        return report;
    }

    /**
     * Calculate threat level based on event characteristics
     */
    private String calculateThreatLevel(SecurityEvent event) {
        if ("REJECTION".equals(event.getEventType())) {
            // Check for attack patterns
            if (isKnownAttackPattern(event)) {
                return "CRITICAL";
            }

            // Check rejection frequency from same IP
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            List<SecurityEvent> recentRejections = eventRepository
                    .findByClientIpAndTimestampBetween(event.getClientIp(), oneHourAgo, LocalDateTime.now())
                    .stream()
                    .filter(e -> "REJECTION".equals(e.getEventType()))
                    .collect(Collectors.toList());

            if (recentRejections.size() >= HIGH_THREAT_REJECTION_COUNT) {
                return "HIGH";
            } else if (recentRejections.size() >= MEDIUM_THREAT_REJECTION_COUNT) {
                return "MEDIUM";
            }
        }

        // Check for slow response times (potential DoS)
        if (event.getResponseTimeMs() != null && event.getResponseTimeMs() > CRITICAL_RESPONSE_TIME) {
            return "HIGH";
        }

        if (event.getClientIp() != null) {
            try {
                GeolocationService.GeolocationData location = geolocationService.getLocation(event.getClientIp());
                ObjectMapper mapper = new ObjectMapper();
                event.setGeoLocation(mapper.writeValueAsString(Map.of(
                        "country", location.getCountry(),
                        "city", location.getCity(),
                        "region", location.getRegion(),
                        "countryCode", location.getCountryCode()
                )));
            } catch (Exception e) {
                log.warn("Failed to get geolocation for IP {}: {}", event.getClientIp(), e.getMessage());
            }
        }

        return "LOW";
    }

    /**
     * Check if the event matches known attack patterns
     */
    private boolean isKnownAttackPattern(SecurityEvent event) {
        String path = event.getRequestPath();
        String userAgent = event.getUserAgent();

        if (path != null) {
            // Common attack patterns
            String[] attackPatterns = {
                    "../", "..\\", "<script", "SELECT ", "UNION ", "DROP ",
                    "INSERT ", "UPDATE ", "DELETE ", "<?php", "eval(", "exec("
            };

            String upperPath = path.toUpperCase();
            for (String pattern : attackPatterns) {
                if (upperPath.contains(pattern.toUpperCase())) {
                    log.warn("Detected attack pattern '{}' in request path: {}", pattern, path);
                    return true;
                }
            }
        }

        if (userAgent != null) {
            // Suspicious user agents
            String[] suspiciousAgents = {
                    "sqlmap", "nikto", "nmap", "masscan", "nessus", "burp", "zap"
            };

            String lowerAgent = userAgent.toLowerCase();
            for (String suspiciousAgent : suspiciousAgents) {
                if (lowerAgent.contains(suspiciousAgent)) {
                    log.warn("Detected suspicious user agent: {}", userAgent);
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Cleanup old events (scheduled task)
     */
    @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM daily
    @Transactional
    public void cleanupOldEvents() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(30); // Keep 30 days of data
            eventRepository.deleteEventsOlderThan(cutoff);
            log.info("Cleaned up security events older than {}", cutoff);
        } catch (Exception e) {
            log.error("Error during security events cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Add metadata to an event
     */
    public void addMetadata(SecurityEvent event, Map<String, Object> metadata) {
        try {
            event.setMetadata(objectMapper.writeValueAsString(metadata));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize metadata for event: {}", e.getMessage());
        }
    }

    /**
     * Get metadata from an event
     */
    public Map<String, Object> getMetadata(SecurityEvent event) {
        try {
            if (event.getMetadata() != null) {
                return objectMapper.readValue(event.getMetadata(), Map.class);
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize metadata for event {}: {}", event.getId(), e.getMessage());
        }
        return new HashMap<>();
    }
}