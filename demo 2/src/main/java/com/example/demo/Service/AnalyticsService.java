package com.example.demo.Service;

import com.example.demo.Filter.RequestCountFilter;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Service for gathering and processing metrics data.
 * In a production environment, this would likely connect to a time-series database
 * like Prometheus, InfluxDB, or use Spring Boot Actuator metrics.
 */
@Service
public class AnalyticsService {

    // Stores request counts per route
    private final ConcurrentHashMap<String, AtomicLong> routeRequestCounter = new ConcurrentHashMap<>();

    // Stores rejection counts per route
    private final ConcurrentHashMap<String, AtomicLong> routeRejectionCounter = new ConcurrentHashMap<>();

    // Stores rejection reasons
    private final ConcurrentHashMap<String, AtomicLong> rejectionReasonCounter = new ConcurrentHashMap<>();

    // Stores response times for routes (in ms)
    private final ConcurrentHashMap<String, List<Long>> routeResponseTimes = new ConcurrentHashMap<>();

    // Time series data for requests (timestamp -> count)
    private final ConcurrentHashMap<Long, Map<String, Long>> timeSeriesRequests = new ConcurrentHashMap<>();

    // Time series data for rejections (timestamp -> count)
    private final ConcurrentHashMap<Long, Map<String, Long>> timeSeriesRejections = new ConcurrentHashMap<>();

    /**
     * Records a request for a specific route
     *
     * @param routeId The ID of the route
     */
    public void recordRequest(String routeId) {
        routeRequestCounter.computeIfAbsent(routeId, k -> new AtomicLong(0)).incrementAndGet();

        // Record in time series data
        long timestamp = System.currentTimeMillis() / 60000; // Per minute
        timeSeriesRequests.computeIfAbsent(timestamp, k -> new ConcurrentHashMap<>())
                .merge(routeId, 1L, Long::sum);
    }

    /**
     * Records a rejection for a specific route
     *
     * @param routeId The ID of the route
     * @param reason The reason for rejection (IP filter, rate limit, etc.)
     */
    public void recordRejection(String routeId, String reason) {
        routeRejectionCounter.computeIfAbsent(routeId, k -> new AtomicLong(0)).incrementAndGet();
        rejectionReasonCounter.computeIfAbsent(reason, k -> new AtomicLong(0)).incrementAndGet();

        // Record in time series data
        long timestamp = System.currentTimeMillis() / 60000; // Per minute
        timeSeriesRejections.computeIfAbsent(timestamp, k -> new ConcurrentHashMap<>())
                .merge(routeId, 1L, Long::sum);
    }

    /**
     * Records response time for a route
     *
     * @param routeId The ID of the route
     * @param responseTimeMs Response time in milliseconds
     */
    public void recordResponseTime(String routeId, long responseTimeMs) {
        routeResponseTimes.computeIfAbsent(routeId, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(responseTimeMs);

        // Keep only last 1000 response times to avoid unbounded memory usage
        List<Long> times = routeResponseTimes.get(routeId);
        if (times.size() > 1000) {
            synchronized (times) {
                if (times.size() > 1000) {
                    times.subList(0, times.size() - 1000).clear();
                }
            }
        }
    }

    /**
     * Gets request counts by route
     *
     * @return Map of route IDs to request counts
     */
    public Map<String, Long> getRequestCountsByRoute() {
        return routeRequestCounter.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().get()
                ));
    }

    /**
     * Gets rejection counts by route
     *
     * @return Map of route IDs to rejection counts
     */
    public Map<String, Long> getRejectionCountsByRoute() {
        return routeRejectionCounter.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().get()
                ));
    }

    /**
     * Gets rejection reason breakdown
     *
     * @return Map of rejection reasons to counts
     */
    public Map<String, Long> getRejectionReasonBreakdown() {
        // If there's no actual data, generate some sample data for demonstration
        if (rejectionReasonCounter.isEmpty()) {
            long totalRejected = RequestCountFilter.getTotalRejectedCount();
            if (totalRejected > 0) {
                rejectionReasonCounter.put("IP Filter", new AtomicLong((long)(totalRejected * 0.4)));
                rejectionReasonCounter.put("Token Validation", new AtomicLong((long)(totalRejected * 0.35)));
                rejectionReasonCounter.put("Rate Limit", new AtomicLong((long)(totalRejected * 0.2)));
                rejectionReasonCounter.put("Invalid Request", new AtomicLong((long)(totalRejected * 0.05)));
            }
        }

        return rejectionReasonCounter.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().get()
                ));
    }

    /**
     * Gets average response time for a route
     *
     * @param routeId The ID of the route
     * @return Average response time in milliseconds
     */
    public double getAverageResponseTime(String routeId) {
        List<Long> times = routeResponseTimes.get(routeId);
        if (times == null || times.isEmpty()) {
            // Generate random response time for demonstration
            return 50 + Math.random() * 200;
        }

        synchronized (times) {
            return times.stream().mapToLong(t -> t).average().orElse(0);
        }
    }

    /**
     * Gets time series data for a specified time range
     *
     * @param minutes Number of minutes to go back in time
     * @param routeId Optional route ID filter (null for all routes)
     * @return List of time series data points
     */
    public List<Map<String, Object>> getTimeSeriesData(int minutes, String routeId) {
        List<Map<String, Object>> result = new ArrayList<>();
        long now = System.currentTimeMillis() / 60000; // Current minute

        // If there's no actual time series data, generate some for demonstration
        if (timeSeriesRequests.isEmpty()) {
            populateSampleTimeSeriesData();
        }

        // Formatter for timestamp display
        DateTimeFormatter formatter;
        if (minutes <= 60) {
            formatter = DateTimeFormatter.ofPattern("HH:mm");
        } else if (minutes <= 24 * 60) {
            formatter = DateTimeFormatter.ofPattern("HH:00");
        } else {
            formatter = DateTimeFormatter.ofPattern("MM-dd");
        }

        // Calculate appropriate interval based on time range
        int interval = 1;
        if (minutes > 60 && minutes <= 24 * 60) {
            interval = 60; // Hourly for 24h view
        } else if (minutes > 24 * 60) {
            interval = 24 * 60; // Daily for 7d view
        }

        // Generate time series data points
        for (long i = now - minutes; i <= now; i += interval) {
            if ((i - (now - minutes)) % interval == 0) { // Only add points at the interval
                Map<String, Object> point = new HashMap<>();

                // Format timestamp
                LocalDateTime dateTime = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(i * 60000),
                        ZoneId.systemDefault()
                );
                point.put("time", dateTime.format(formatter));

                // Get request and rejection counts
                Map<String, Long> requests = timeSeriesRequests.getOrDefault(i, Collections.emptyMap());
                Map<String, Long> rejections = timeSeriesRejections.getOrDefault(i, Collections.emptyMap());

                // Filter by route if specified
                if (routeId != null && !routeId.equals("all")) {
                    point.put("requests", requests.getOrDefault(routeId, 0L));
                    point.put("rejected", rejections.getOrDefault(routeId, 0L));
                    point.put("accepted", (long) Math.max(0, requests.getOrDefault(routeId, 0L) -
                            rejections.getOrDefault(routeId, 0L)));
                } else {
                    // Sum across all routes
                    long totalRequests = requests.values().stream().mapToLong(Long::longValue).sum();
                    long totalRejections = rejections.values().stream().mapToLong(Long::longValue).sum();
                    point.put("requests", totalRequests);
                    point.put("rejected", totalRejections);
                    point.put("accepted", Math.max(0, totalRequests - totalRejections));
                }

                result.add(point);
            }
        }

        return result;
    }

    /**
     * Generates sample time series data for demonstration purposes.
     * In a real implementation, this would come from actual monitoring.
     */
    private void populateSampleTimeSeriesData() {
        long now = System.currentTimeMillis() / 60000; // Current minute
        Random random = new Random();

        // Get current request counts from RequestCountFilter
        RequestCountFilter.MinuteMetrics currentMetrics = RequestCountFilter.getMinuteMetrics();
        long currentRequests = currentMetrics.getRequestsCurrentMinute();
        long currentRejected = currentMetrics.getRejectedCurrentMinute();

        // Generate 24 hours of data (1440 minutes)
        for (int i = 0; i < 1440; i++) {
            long timestamp = now - i;

            // Base values decline as we go further back in time
            double timeFactor = Math.max(0.5, 1.0 - (i / 2880.0)); // 0.5 to 1.0

            // Randomized values around the current metrics
            long requestValue = currentRequests > 0
                    ? (long)(currentRequests * timeFactor * (0.5 + random.nextDouble()))
                    : (long)(10 * timeFactor * (0.5 + random.nextDouble()));

            long rejectedValue = currentRejected > 0
                    ? (long)(currentRejected * timeFactor * (0.5 + random.nextDouble()))
                    : (long)(2 * timeFactor * (0.5 + random.nextDouble()));

            // Distribute among routes - in real impl this would be based on actual routes
            Map<String, Long> routeRequests = new HashMap<>();
            Map<String, Long> routeRejections = new HashMap<>();

            routeRequests.put("route-1", (long)(requestValue * 0.4));
            routeRequests.put("route-2", (long)(requestValue * 0.3));
            routeRequests.put("route-3", (long)(requestValue * 0.2));
            routeRequests.put("route-4", requestValue -
                    routeRequests.get("route-1") -
                    routeRequests.get("route-2") -
                    routeRequests.get("route-3"));

            routeRejections.put("route-1", (long)(rejectedValue * 0.25));
            routeRejections.put("route-2", (long)(rejectedValue * 0.35));
            routeRejections.put("route-3", (long)(rejectedValue * 0.3));
            routeRejections.put("route-4", rejectedValue -
                    routeRejections.get("route-1") -
                    routeRejections.get("route-2") -
                    routeRejections.get("route-3"));

            timeSeriesRequests.put(timestamp, routeRequests);
            timeSeriesRejections.put(timestamp, routeRejections);
        }
    }

    /**
     * Cleans up old time series data to avoid unbounded memory usage
     */
    public void cleanupOldData() {
        long cutoff = System.currentTimeMillis() / 60000 - 7 * 24 * 60; // 7 days ago

        timeSeriesRequests.keySet().removeIf(timestamp -> timestamp < cutoff);
        timeSeriesRejections.keySet().removeIf(timestamp -> timestamp < cutoff);

        // Also trim the response time lists to avoid memory leaks
        routeResponseTimes.forEach((route, times) -> {
            synchronized (times) {
                if (times.size() > 1000) {
                    times.subList(0, times.size() - 1000).clear();
                }
            }
        });
    }
}