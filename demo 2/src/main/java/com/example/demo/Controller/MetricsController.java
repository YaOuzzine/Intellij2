package com.example.demo.Controller;

import com.example.demo.Entity.GatewayRoute;
import com.example.demo.Filter.RequestCountFilter;
import com.example.demo.Repository.GatewayRouteRepository;
import com.example.demo.Service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class MetricsController {

    private final GatewayRouteRepository gatewayRouteRepository;
    private final AnalyticsService analyticsService;

    @Autowired
    public MetricsController(GatewayRouteRepository gatewayRouteRepository, AnalyticsService analyticsService) {
        this.gatewayRouteRepository = gatewayRouteRepository;
        this.analyticsService = analyticsService;
    }

    // Endpoint for total request counts.
    @GetMapping("/api/metrics/requests")
    public RequestCountResponse getTotalRequests() {
        return new RequestCountResponse(
                RequestCountFilter.getTotalRequestCount(),
                RequestCountFilter.getTotalRejectedCount()
        );
    }

    // DTO for total request counts.
    public static class RequestCountResponse {
        private final long requestCount;
        private final long rejectedCount;

        public RequestCountResponse(long requestCount, long rejectedCount) {
            this.requestCount = requestCount;
            this.rejectedCount = rejectedCount;
        }

        public long getRequestCount() {
            return requestCount;
        }

        public long getRejectedCount() {
            return rejectedCount;
        }
    }

    // Endpoint to expose per-minute metrics.
    @GetMapping("/api/metrics/minutely")
    public MinuteMetricsResponse getMinutelyMetrics() {
        RequestCountFilter.MinuteMetrics metrics = RequestCountFilter.getMinuteMetrics();
        return new MinuteMetricsResponse(
                metrics.getRequestsCurrentMinute(),
                metrics.getRequestsPreviousMinute(),
                metrics.getRejectedCurrentMinute(),
                metrics.getRejectedPreviousMinute()
        );
    }

    // DTO for per-minute metrics.
    public static class MinuteMetricsResponse {
        private final long requestsCurrentMinute;
        private final long requestsPreviousMinute;
        private final long rejectedCurrentMinute;
        private final long rejectedPreviousMinute;

        public MinuteMetricsResponse(long requestsCurrentMinute, long requestsPreviousMinute,
                                     long rejectedCurrentMinute, long rejectedPreviousMinute) {
            this.requestsCurrentMinute = requestsCurrentMinute;
            this.requestsPreviousMinute = requestsPreviousMinute;
            this.rejectedCurrentMinute = rejectedCurrentMinute;
            this.rejectedPreviousMinute = rejectedPreviousMinute;
        }

        public long getRequestsCurrentMinute() {
            return requestsCurrentMinute;
        }

        public long getRequestsPreviousMinute() {
            return requestsPreviousMinute;
        }

        public long getRejectedCurrentMinute() {
            return rejectedCurrentMinute;
        }

        public long getRejectedPreviousMinute() {
            return rejectedPreviousMinute;
        }
    }

    // New endpoint for route-specific analytics
    @GetMapping("/api/metrics/routes")
    public List<RouteMetricsResponse> getRouteMetrics() {
        List<GatewayRoute> routes = gatewayRouteRepository.findAll();
        Map<String, Long> routeMetrics = analyticsService.getRequestCountsByRoute();
        Map<String, Long> rejectionMetrics = analyticsService.getRejectionCountsByRoute();

        return routes.stream().map(route -> {
            String routeId = route.getRouteId() != null ? route.getRouteId() : "route-" + route.getId();
            long requestCount = routeMetrics.getOrDefault(routeId, 0L);
            long rejectedCount = rejectionMetrics.getOrDefault(routeId, 0L);

            return new RouteMetricsResponse(
                    route.getId(),
                    routeId,
                    route.getPredicates(),
                    requestCount,
                    rejectedCount,
                    analyticsService.getAverageResponseTime(routeId)
            );
        }).collect(Collectors.toList());
    }

    // DTO for route metrics
    public static class RouteMetricsResponse {
        private final Long id;
        private final String routeId;
        private final String predicates;
        private final long requestCount;
        private final long rejectedCount;
        private final double avgResponseTime;

        public RouteMetricsResponse(Long id, String routeId, String predicates,
                                    long requestCount, long rejectedCount, double avgResponseTime) {
            this.id = id;
            this.routeId = routeId;
            this.predicates = predicates;
            this.requestCount = requestCount;
            this.rejectedCount = rejectedCount;
            this.avgResponseTime = avgResponseTime;
        }

        public Long getId() {
            return id;
        }

        public String getRouteId() {
            return routeId;
        }

        public String getPredicates() {
            return predicates;
        }

        public long getRequestCount() {
            return requestCount;
        }

        public long getRejectedCount() {
            return rejectedCount;
        }

        public double getAvgResponseTime() {
            return avgResponseTime;
        }
    }

    // New endpoint for rejection reasons analytics
    @GetMapping("/api/metrics/rejections")
    public RejectionBreakdownResponse getRejectionBreakdown() {
        Map<String, Long> breakdownMap = analyticsService.getRejectionReasonBreakdown();
        return new RejectionBreakdownResponse(breakdownMap);
    }

    // DTO for rejection breakdowns
    public static class RejectionBreakdownResponse {
        private final Map<String, Long> rejectionReasons;

        public RejectionBreakdownResponse(Map<String, Long> rejectionReasons) {
            this.rejectionReasons = rejectionReasons;
        }

        public Map<String, Long> getRejectionReasons() {
            return rejectionReasons;
        }
    }

    // New endpoint for time series data
    @GetMapping("/api/metrics/timeseries")
    public TimeSeriesResponse getTimeSeriesData(
            @RequestParam(value = "timeRange", defaultValue = "1h") String timeRange,
            @RequestParam(value = "routeId", required = false) String routeId) {

        // Convert timeRange to minutes
        int minutes;
        switch (timeRange) {
            case "24h":
                minutes = 24 * 60;
                break;
            case "7d":
                minutes = 7 * 24 * 60;
                break;
            case "1h":
            default:
                minutes = 60;
                break;
        }

        // Get time series data from analytics service
        List<Map<String, Object>> timeSeriesData = analyticsService.getTimeSeriesData(minutes, routeId);

        return new TimeSeriesResponse(timeSeriesData);
    }

    // DTO for time series data
    public static class TimeSeriesResponse {
        private final List<Map<String, Object>> timeSeries;

        public TimeSeriesResponse(List<Map<String, Object>> timeSeries) {
            this.timeSeries = timeSeries;
        }

        public List<Map<String, Object>> getTimeSeries() {
            return timeSeries;
        }
    }
}