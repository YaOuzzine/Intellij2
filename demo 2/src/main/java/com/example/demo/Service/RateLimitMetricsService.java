package com.example.demo.Service;

import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Service for tracking detailed rate limit metrics.
 * This service maintains counters for:
 * - Requests per route
 * - Rate limit rejections per route
 * - Historical data for visualization
 */
@Service
public class RateLimitMetricsService {

    // Map to track requests per route (routeId -> counter)
    private final Map<String, Long> requestsPerRoute = new ConcurrentHashMap<>();

    // Map to track rate limit rejections per route (routeId -> counter)
    private final Map<String, Long> rejectionsPerRoute = new ConcurrentHashMap<>();

    // Historical data for charts (routeId -> list of time-based records)
    private final Map<String, List<TimeSeriesRecord>> historicalData = new ConcurrentHashMap<>();

    // Maximum number of historical records to keep per route
    private static final int MAX_HISTORY_RECORDS = 60; // 60 minutes of data

    /**
     * Record a request for a specific route
     */
    public void recordRequest(String routeId) {
        requestsPerRoute.compute(routeId, (k, v) -> (v == null) ? 1 : v + 1);
    }

    /**
     * Record a rate limit rejection for a specific route
     */
    public void recordRejection(String routeId) {
        rejectionsPerRoute.compute(routeId, (k, v) -> (v == null) ? 1 : v + 1);
    }

    /**
     * Get the current count of requests for a route
     */
    public long getRequestCount(String routeId) {
        return requestsPerRoute.getOrDefault(routeId, 0L);
    }

    /**
     * Get the current count of rejections for a route
     */
    public long getRejectionCount(String routeId) {
        return rejectionsPerRoute.getOrDefault(routeId, 0L);
    }

    /**
     * Get all route metrics
     */
    public Map<String, RouteMetrics> getAllRouteMetrics() {
        Map<String, RouteMetrics> result = new ConcurrentHashMap<>();

        // Combine both maps to get all routeIds
        Set<String> allRouteIds = new HashSet<>();
        allRouteIds.addAll(requestsPerRoute.keySet());
        allRouteIds.addAll(rejectionsPerRoute.keySet());

        for (String routeId : allRouteIds) {
            long requests = requestsPerRoute.getOrDefault(routeId, 0L);
            long rejections = rejectionsPerRoute.getOrDefault(routeId, 0L);
            List<TimeSeriesRecord> history = historicalData.getOrDefault(routeId,
                    Collections.emptyList());

            result.put(routeId, new RouteMetrics(requests, rejections, history));
        }

        return result;
    }

    /**
     * Get historical data for a specific route
     */
    public List<TimeSeriesRecord> getHistoricalData(String routeId) {
        return historicalData.getOrDefault(routeId, Collections.emptyList());
    }

    /**
     * Scheduled task to record historical data points every minute
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void recordHistoricalDataPoint() {
        Instant now = Instant.now();

        // Record data for each route we're tracking
        Set<String> allRouteIds = new HashSet<>();
        allRouteIds.addAll(requestsPerRoute.keySet());
        allRouteIds.addAll(rejectionsPerRoute.keySet());

        for (String routeId : allRouteIds) {
            long requests = requestsPerRoute.getOrDefault(routeId, 0L);
            long rejections = rejectionsPerRoute.getOrDefault(routeId, 0L);

            // Create a new record
            TimeSeriesRecord record = new TimeSeriesRecord(now, requests, rejections);

            // Add to historical data
            historicalData.computeIfAbsent(routeId, k -> new ArrayList<>()).add(record);

            // Limit the number of records
            List<TimeSeriesRecord> records = historicalData.get(routeId);
            if (records.size() > MAX_HISTORY_RECORDS) {
                records.remove(0); // Remove oldest
            }

            // Reset counters for the next minute
            requestsPerRoute.put(routeId, 0L);
            rejectionsPerRoute.put(routeId, 0L);
        }
    }

    /**
     * Class to represent a time-series data point
     */
    public static class TimeSeriesRecord {
        private final Instant timestamp;
        private final long requests;
        private final long rejections;

        public TimeSeriesRecord(Instant timestamp, long requests, long rejections) {
            this.timestamp = timestamp;
            this.requests = requests;
            this.rejections = rejections;
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        public long getRequests() {
            return requests;
        }

        public long getRejections() {
            return rejections;
        }
    }

    /**
     * Class to represent metrics for a route
     */
    public static class RouteMetrics {
        private final long totalRequests;
        private final long totalRejections;
        private final List<TimeSeriesRecord> history;

        public RouteMetrics(long totalRequests, long totalRejections,
                            List<TimeSeriesRecord> history) {
            this.totalRequests = totalRequests;
            this.totalRejections = totalRejections;
            this.history = history;
        }

        public long getTotalRequests() {
            return totalRequests;
        }

        public long getTotalRejections() {
            return totalRejections;
        }

        public List<TimeSeriesRecord> getHistory() {
            return history;
        }
    }
}