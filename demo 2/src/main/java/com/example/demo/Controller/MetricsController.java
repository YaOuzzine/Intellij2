// demo 2/src/main/java/com/example/demo/Controller/MetricsController.java
package com.example.demo.Controller;

import com.example.demo.Filter.RequestCountFilter;
import com.example.demo.Service.AnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/metrics")
@CrossOrigin(origins = "http://localhost:5173")
public class MetricsController {

    private static final Logger log = LoggerFactory.getLogger(MetricsController.class);
    private final AnalyticsService analyticsService;

    @Autowired
    public MetricsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Get basic request metrics (total counts)
     */
    @GetMapping("/requests")
    public ResponseEntity<Map<String, Object>> getRequestMetrics(
            @RequestParam(required = false) String routeId) {
        try {
            log.debug("Fetching request metrics for routeId: {}", routeId);

            Map<String, Object> metrics = new HashMap<>();

            if (routeId != null && !routeId.isEmpty() && !"all".equals(routeId)) {
                // Get route-specific metrics from analytics service
                Map<String, Object> routeAnalytics = analyticsService.getRouteAnalyticsWithAI(routeId);

                if (routeAnalytics.containsKey("error")) {
                    metrics.put("requestCount", 0);
                    metrics.put("rejectedCount", 0);
                } else {
                    metrics.put("requestCount", routeAnalytics.getOrDefault("totalRequests", 0));
                    metrics.put("rejectedCount", routeAnalytics.getOrDefault("totalRejections", 0));
                }
            } else {
                // Get global metrics from RequestCountFilter
                metrics.put("requestCount", RequestCountFilter.getTotalRequestCount());
                metrics.put("rejectedCount", RequestCountFilter.getTotalRejectedCount());
            }

            metrics.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            return ResponseEntity.ok(metrics);

        } catch (Exception e) {
            log.error("Error fetching request metrics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch request metrics", "message", e.getMessage()));
        }
    }

    /**
     * Get minutely metrics (current and previous minute)
     */
    @GetMapping("/minutely")
    public ResponseEntity<Map<String, Object>> getMinutelyMetrics(
            @RequestParam(required = false) String routeId) {
        try {
            log.debug("Fetching minutely metrics for routeId: {}", routeId);

            Map<String, Object> metrics = new HashMap<>();

            if (routeId != null && !routeId.isEmpty() && !"all".equals(routeId)) {
                // For route-specific minutely data, we'll need to implement this in AnalyticsService
                // For now, return zeros as route-specific minutely tracking isn't implemented yet
                metrics.put("requestsCurrentMinute", 0);
                metrics.put("requestsPreviousMinute", 0);
                metrics.put("rejectedCurrentMinute", 0);
                metrics.put("rejectedPreviousMinute", 0);
            } else {
                // Get global minutely metrics from RequestCountFilter
                RequestCountFilter.MinuteMetrics minuteMetrics = RequestCountFilter.getMinuteMetrics();
                metrics.put("requestsCurrentMinute", minuteMetrics.getRequestsCurrentMinute());
                metrics.put("requestsPreviousMinute", minuteMetrics.getRequestsPreviousMinute());
                metrics.put("rejectedCurrentMinute", minuteMetrics.getRejectedCurrentMinute());
                metrics.put("rejectedPreviousMinute", minuteMetrics.getRejectedPreviousMinute());
            }

            metrics.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            return ResponseEntity.ok(metrics);

        } catch (Exception e) {
            log.error("Error fetching minutely metrics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch minutely metrics", "message", e.getMessage()));
        }
    }

    /**
     * Get rejection reasons breakdown
     */
    @GetMapping("/rejections")
    public ResponseEntity<Map<String, Object>> getRejectionMetrics(
            @RequestParam(required = false) String routeId) {
        try {
            log.debug("Fetching rejection metrics for routeId: {}", routeId);

            Map<String, Object> metrics = new HashMap<>();

            if (routeId != null && !routeId.isEmpty() && !"all".equals(routeId)) {
                // Get route-specific rejection reasons from analytics service
                Map<String, Object> routeAnalytics = analyticsService.getRouteAnalyticsWithAI(routeId);

                if (routeAnalytics.containsKey("rejectionReasons")) {
                    Object rejectionReasons = routeAnalytics.get("rejectionReasons");
                    if (rejectionReasons instanceof Map) {
                        metrics.put("rejectionReasons", rejectionReasons);
                    } else {
                        metrics.put("rejectionReasons", new HashMap<>());
                    }
                } else {
                    metrics.put("rejectionReasons", new HashMap<>());
                }
            } else {
                // Get global rejection reasons from RequestCountFilter
                ConcurrentHashMap<String, String> globalReasons = RequestCountFilter.getRejectionReasons();

                // Convert to reason -> count mapping
                Map<String, Long> reasonCounts = globalReasons.values().stream()
                        .collect(Collectors.groupingBy(
                                reason -> reason,
                                Collectors.counting()
                        ));

                metrics.put("rejectionReasons", reasonCounts);
            }

            metrics.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            return ResponseEntity.ok(metrics);

        } catch (Exception e) {
            log.error("Error fetching rejection metrics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch rejection metrics", "message", e.getMessage()));
        }
    }

    /**
     * Get time series data for charts
     */
    @GetMapping("/timeseries")
    public ResponseEntity<Map<String, Object>> getTimeSeriesData(
            @RequestParam(defaultValue = "1h") String timeRange,
            @RequestParam(required = false) String routeId) {
        try {
            log.debug("Fetching time series data for timeRange: {}, routeId: {}", timeRange, routeId);

            Map<String, Object> response = new HashMap<>();
            List<Map<String, Object>> timeSeries = new ArrayList<>();

            // Generate sample time series data since we don't have persistent storage yet
            // In a real implementation, this would query the SecurityEvent repository
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime start = calculateStartTime(now, timeRange);

            // Generate data points every 5 minutes for the requested time range
            LocalDateTime current = start;
            while (current.isBefore(now)) {
                Map<String, Object> dataPoint = new HashMap<>();
                dataPoint.put("time", current.format(DateTimeFormatter.ofPattern("HH:mm")));

                // Simulate some realistic data with variation
                int baseRequests = 10 + (int)(Math.random() * 20);
                int rejections = (int)(Math.random() * 5);

                dataPoint.put("total", baseRequests + rejections);
                dataPoint.put("accepted", baseRequests);
                dataPoint.put("rejected", rejections);
                dataPoint.put("avgResponseTime", 200 + (int)(Math.random() * 300));

                timeSeries.add(dataPoint);
                current = current.plusMinutes(5);
            }

            response.put("timeSeries", timeSeries);
            response.put("timeRange", timeRange);
            response.put("routeId", routeId != null ? routeId : "all");
            response.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching time series data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch time series data", "message", e.getMessage()));
        }
    }

    /**
     * Get comprehensive analytics dashboard data
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardMetrics() {
        try {
            log.info("Fetching comprehensive dashboard metrics");

            // Get enhanced dashboard data from analytics service
            Map<String, Object> dashboardData = analyticsService.getEnhancedDashboardData();

            return ResponseEntity.ok(dashboardData);

        } catch (Exception e) {
            log.error("Error fetching dashboard metrics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch dashboard metrics", "message", e.getMessage()));
        }
    }

    /**
     * Get health status of metrics system
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getMetricsHealth() {
        Map<String, Object> health = new HashMap<>();

        try {
            // Check if analytics service is working
            RequestCountFilter.MinuteMetrics metrics = RequestCountFilter.getMinuteMetrics();

            health.put("status", "HEALTHY");
            health.put("analyticsService", analyticsService != null ? "AVAILABLE" : "UNAVAILABLE");
            health.put("totalRequests", RequestCountFilter.getTotalRequestCount());
            health.put("totalRejections", RequestCountFilter.getTotalRejectedCount());
            health.put("currentMinuteRequests", metrics.getRequestsCurrentMinute());
            health.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            log.error("Error checking metrics health: {}", e.getMessage(), e);
            health.put("status", "UNHEALTHY");
            health.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
        }
    }

    /**
     * Reset metrics (for testing purposes)
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetMetrics() {
        try {
            log.warn("Metrics reset requested - this should only be used for testing");

            // Note: We can't actually reset the static counters in RequestCountFilter
            // without adding a reset method there. This is just a placeholder.

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Metrics reset requested");
            response.put("note", "Static counters cannot be reset without service restart");
            response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error resetting metrics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to reset metrics", "message", e.getMessage()));
        }
    }

    /**
     * Calculate start time based on time range
     */
    private LocalDateTime calculateStartTime(LocalDateTime now, String timeRange) {
        switch (timeRange) {
            case "1h":
                return now.minusHours(1);
            case "24h":
                return now.minusHours(24);
            case "7d":
                return now.minusDays(7);
            default:
                return now.minusHours(1);
        }
    }
}