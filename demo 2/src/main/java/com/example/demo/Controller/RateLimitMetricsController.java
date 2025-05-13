package com.example.demo.Controller;

import com.example.demo.Entity.GatewayRoute;
import com.example.demo.Repository.GatewayRouteRepository;
import com.example.demo.Service.RateLimitMetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/metrics/ratelimit")
public class RateLimitMetricsController {

    private final RateLimitMetricsService metricsService;
    private final GatewayRouteRepository routeRepository;

    @Autowired
    public RateLimitMetricsController(RateLimitMetricsService metricsService,
                                      GatewayRouteRepository routeRepository) {
        this.metricsService = metricsService;
        this.routeRepository = routeRepository;
    }

    /**
     * Get metrics for all routes
     */
    @GetMapping
    public Map<String, Object> getAllMetrics() {
        Map<String, Object> result = new HashMap<>();

        // Get all routes with their metrics
        Map<String, RateLimitMetricsService.RouteMetrics> routeMetrics = metricsService.getAllRouteMetrics();

        // Get route details for UI display
        List<GatewayRoute> routes = routeRepository.findAll();

        // Create response with route details
        List<Map<String, Object>> routesList = new ArrayList<>();

        for (GatewayRoute route : routes) {
            String routeId = route.getRouteId() != null ?
                    route.getRouteId() : "route-" + route.getId();

            Map<String, Object> routeData = new HashMap<>();
            routeData.put("id", route.getId());
            routeData.put("routeId", routeId);
            routeData.put("path", route.getPredicates());
            routeData.put("uri", route.getUri());
            routeData.put("withRateLimit", route.getWithRateLimit());

            if (route.getRateLimit() != null) {
                Map<String, Object> rateLimitData = new HashMap<>();
                rateLimitData.put("maxRequests", route.getRateLimit().getMaxRequests());
                rateLimitData.put("timeWindowMs", route.getRateLimit().getTimeWindowMs());
                routeData.put("rateLimit", rateLimitData);
            }

            // Add metrics data if available
            RateLimitMetricsService.RouteMetrics metrics = routeMetrics.get(routeId);
            if (metrics != null) {
                routeData.put("totalRequests", metrics.getTotalRequests());
                routeData.put("totalRejections", metrics.getTotalRejections());

                // Calculate rejection percentage
                double rejectionRate = metrics.getTotalRequests() > 0 ?
                        (double) metrics.getTotalRejections() / metrics.getTotalRequests() * 100 : 0;
                routeData.put("rejectionRate", Math.round(rejectionRate * 100) / 100.0);

                // Get last 5 minutes of history for preview
                List<RateLimitMetricsService.TimeSeriesRecord> history = metrics.getHistory();
                if (history != null && !history.isEmpty()) {
                    int historySize = history.size();
                    int startIndex = Math.max(0, historySize - 5);
                    List<Map<String, Object>> recentHistory = new ArrayList<>();

                    for (int i = startIndex; i < historySize; i++) {
                        RateLimitMetricsService.TimeSeriesRecord record = history.get(i);
                        Map<String, Object> point = new HashMap<>();
                        point.put("timestamp", record.getTimestamp().toEpochMilli());
                        point.put("requests", record.getRequests());
                        point.put("rejections", record.getRejections());
                        recentHistory.add(point);
                    }

                    routeData.put("recentHistory", recentHistory);
                }
            } else {
                // Default values if no metrics found
                routeData.put("totalRequests", 0);
                routeData.put("totalRejections", 0);
                routeData.put("rejectionRate", 0);
            }

            routesList.add(routeData);
        }

        result.put("routes", routesList);

        // Add summary metrics
        long totalRequests = routeMetrics.values().stream()
                .mapToLong(RateLimitMetricsService.RouteMetrics::getTotalRequests)
                .sum();

        long totalRejections = routeMetrics.values().stream()
                .mapToLong(RateLimitMetricsService.RouteMetrics::getTotalRejections)
                .sum();

        double overallRejectionRate = totalRequests > 0 ?
                (double) totalRejections / totalRequests * 100 : 0;

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalRequests", totalRequests);
        summary.put("totalRejections", totalRejections);
        summary.put("rejectionRate", Math.round(overallRejectionRate * 100) / 100.0);

        result.put("summary", summary);

        return result;
    }

    /**
     * Get detailed history for a specific route
     */
    @GetMapping("/{routeId}/history")
    public Map<String, Object> getRouteHistory(@PathVariable String routeId) {
        Map<String, Object> result = new HashMap<>();

        // Find the route first to verify it exists
        GatewayRoute route = null;
        List<GatewayRoute> routes = routeRepository.findAll();

        for (GatewayRoute r : routes) {
            String id = r.getRouteId() != null ? r.getRouteId() : "route-" + r.getId();
            if (id.equals(routeId)) {
                route = r;
                break;
            }
        }

        if (route == null) {
            // Return empty result if route not found
            result.put("history", new ArrayList<>());
            return result;
        }

        // Get historical data
        List<RateLimitMetricsService.TimeSeriesRecord> history =
                metricsService.getHistoricalData(routeId);

        // Convert to API-friendly format
        List<Map<String, Object>> historyData = history.stream().map(record -> {
            Map<String, Object> point = new HashMap<>();
            point.put("timestamp", record.getTimestamp().toEpochMilli());
            point.put("requests", record.getRequests());
            point.put("rejections", record.getRejections());
            return point;
        }).collect(Collectors.toList());

        result.put("routeId", routeId);
        result.put("path", route.getPredicates());

        if (route.getRateLimit() != null) {
            result.put("maxRequests", route.getRateLimit().getMaxRequests());
            result.put("timeWindowMs", route.getRateLimit().getTimeWindowMs());
        }

        result.put("history", historyData);

        return result;
    }
}