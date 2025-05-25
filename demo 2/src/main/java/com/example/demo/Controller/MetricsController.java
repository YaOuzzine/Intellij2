// demo 2/src/main/java/com/example/demo/Controller/MetricsController.java
package com.example.demo.Controller;

import com.example.demo.Filter.RequestCountFilter;
import com.example.demo.Service.AnalyticsService;
import com.example.demo.Repository.SecurityEventRepository;
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
    private final SecurityEventRepository eventRepository;

    @Autowired
    public MetricsController(AnalyticsService analyticsService, SecurityEventRepository eventRepository) {
        this.analyticsService = analyticsService;
        this.eventRepository = eventRepository;
    }

    @GetMapping("/requests")
    public ResponseEntity<Map<String, Object>> getRequestMetrics(
            @RequestParam(required = false) String routeId) {
        try {
            log.debug("Fetching request metrics for routeId: {}", routeId);

            Map<String, Object> metrics = new HashMap<>();

            if (routeId != null && !routeId.isEmpty() && !"all".equals(routeId)) {
                Map<String, Object> routeAnalytics = analyticsService.getRouteAnalyticsWithAI(routeId);

                if (routeAnalytics.containsKey("error")) {
                    metrics.put("requestCount", 0);
                    metrics.put("rejectedCount", 0);
                } else {
                    metrics.put("requestCount", routeAnalytics.getOrDefault("totalRequests", 0));
                    metrics.put("rejectedCount", routeAnalytics.getOrDefault("totalRejections", 0));
                }
            } else {
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

    @GetMapping("/minutely")
    public ResponseEntity<Map<String, Object>> getMinutelyMetrics(
            @RequestParam(required = false) String routeId) {
        try {
            log.debug("Fetching minutely metrics for routeId: {}", routeId);

            Map<String, Object> metrics = new HashMap<>();

            if (routeId != null && !routeId.isEmpty() && !"all".equals(routeId)) {
                // Route-specific minutely data not implemented yet - return zeros
                metrics.put("requestsCurrentMinute", 0);
                metrics.put("requestsPreviousMinute", 0);
                metrics.put("rejectedCurrentMinute", 0);
                metrics.put("rejectedPreviousMinute", 0);
            } else {
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

    @GetMapping("/rejections")
    public ResponseEntity<Map<String, Object>> getRejectionMetrics(
            @RequestParam(required = false) String routeId) {
        try {
            log.debug("Fetching rejection metrics for routeId: {}", routeId);

            Map<String, Object> metrics = new HashMap<>();

            if (routeId != null && !routeId.isEmpty() && !"all".equals(routeId)) {
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
                ConcurrentHashMap<String, String> globalReasons = RequestCountFilter.getRejectionReasons();

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

    // REMOVED: getTimeSeriesData() method - was generating fake data
    // Real time series data should come from SecurityEventRepository queries

    @GetMapping("/timeseries")
    public ResponseEntity<Map<String, Object>> getTimeSeriesData(
            @RequestParam(defaultValue = "1h") String timeRange,
            @RequestParam(required = false) String routeId) {
        try {
            log.debug("Fetching real time series data for timeRange: {}, routeId: {}", timeRange, routeId);

            Map<String, Object> response = new HashMap<>();

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime start = calculateStartTime(now, timeRange);

            // Get real data from database
            List<Object[]> hourlyData = eventRepository.getHourlyEventCounts(start);

            List<Map<String, Object>> timeSeries = new ArrayList<>();
            for (Object[] row : hourlyData) {
                Map<String, Object> dataPoint = new HashMap<>();
                dataPoint.put("time", row[0].toString());
                dataPoint.put("total", row[1]);

                // Get rejection data for the same time period
                // This would need additional repository method for rejections by hour
                dataPoint.put("rejected", 0); // Placeholder until rejection tracking by hour is implemented
                dataPoint.put("accepted", (Long)row[1] - 0); // total - rejected

                timeSeries.add(dataPoint);
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

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardMetrics() {
        try {
            log.info("Fetching comprehensive dashboard metrics");
            Map<String, Object> dashboardData = analyticsService.getEnhancedDashboardData();
            return ResponseEntity.ok(dashboardData);
        } catch (Exception e) {
            log.error("Error fetching dashboard metrics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch dashboard metrics", "message", e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getMetricsHealth() {
        Map<String, Object> health = new HashMap<>();

        try {
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