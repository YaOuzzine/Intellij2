// demo 2/src/main/java/com/example/demo/Service/AnalyticsService.java
package com.example.demo.Service;

import com.example.demo.Filter.RequestCountFilter;
import com.fasterxml.jackson.core.JsonProcessingException; // Added for logging
import com.fasterxml.jackson.databind.ObjectMapper; // Added for logging
import org.slf4j.Logger; // Added for logging
import org.slf4j.LoggerFactory; // Added for logging
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    // Stores request counts per route
    private final ConcurrentHashMap<String, AtomicLong> routeRequestCounter = new ConcurrentHashMap<>();
    // Stores rejection counts per route
    private final ConcurrentHashMap<String, AtomicLong> routeRejectionCounter = new ConcurrentHashMap<>();
    // Stores rejection reasons
    private final ConcurrentHashMap<String, AtomicLong> rejectionReasonCounter = new ConcurrentHashMap<>();
    // Stores response times for routes (in ms)
    private final ConcurrentHashMap<String, List<Long>> routeResponseTimes = new ConcurrentHashMap<>();

    // Time series data for requests (timestamp_minute_epoch -> map_of_routeId_to_count)
    private final ConcurrentHashMap<Long, Map<String, Long>> timeSeriesRequests = new ConcurrentHashMap<>();
    // Time series data for rejections (timestamp_minute_epoch -> map_of_routeId_to_count)
    private final ConcurrentHashMap<Long, Map<String, Long>> timeSeriesRejections = new ConcurrentHashMap<>();

    // Maximum number of historical records to keep per route
    private static final int MAX_HISTORY_RECORDS = 60; // 60 minutes of data

    private final ObjectMapper objectMapper = new ObjectMapper(); // For logging complex objects

    public AnalyticsService() {
        log.info("[AnalyticsService] Initialized. Real data only - no sample data generation.");
    }

    public void recordRequest(String routeId) {
        log.trace("[AnalyticsService] recordRequest called for routeId: '{}'", routeId);
        routeRequestCounter.computeIfAbsent(routeId, k -> new AtomicLong(0)).incrementAndGet();

        long timestampMinuteEpoch = System.currentTimeMillis() / 60000; // Per minute
        timeSeriesRequests.computeIfAbsent(timestampMinuteEpoch, k -> new ConcurrentHashMap<>())
                .merge(routeId, 1L, Long::sum);
        log.debug("[AnalyticsService] Recorded request for routeId: '{}' at timestamp (minute epoch): {}. Current total for route: {}",
                routeId, timestampMinuteEpoch, routeRequestCounter.get(routeId).get());
    }

    public void recordRejection(String routeId, String reason) {
        log.trace("[AnalyticsService] recordRejection called for routeId: '{}', reason: '{}'", routeId, reason);
        routeRejectionCounter.computeIfAbsent(routeId, k -> new AtomicLong(0)).incrementAndGet();
        rejectionReasonCounter.computeIfAbsent(reason, k -> new AtomicLong(0)).incrementAndGet();

        long timestampMinuteEpoch = System.currentTimeMillis() / 60000; // Per minute
        timeSeriesRejections.computeIfAbsent(timestampMinuteEpoch, k -> new ConcurrentHashMap<>())
                .merge(routeId, 1L, Long::sum);
        log.debug("[AnalyticsService] Recorded rejection for routeId: '{}', reason: '{}' at timestamp (minute epoch): {}. Current total rejections for route: {}",
                routeId, reason, timestampMinuteEpoch, routeRejectionCounter.get(routeId).get());
    }

    public void recordResponseTime(String routeId, long responseTimeMs) {
        log.trace("[AnalyticsService] recordResponseTime called for routeId: '{}', time: {}ms", routeId, responseTimeMs);
        List<Long> times = routeResponseTimes.computeIfAbsent(routeId, k -> Collections.synchronizedList(new ArrayList<>()));
        times.add(responseTimeMs);

        // Keep only last 1000 response times to avoid unbounded memory usage
        if (times.size() > 1000) {
            synchronized (times) { // Ensure thread-safety for subList and clear
                if (times.size() > 1000) {
                    times.subList(0, times.size() - 1000).clear();
                    log.trace("[AnalyticsService] Trimmed response times for routeId: '{}'. New size: {}", routeId, times.size());
                }
            }
        }
    }

    public Map<String, Long> getRequestCountsByRoute() {
        Map<String, Long> counts = routeRequestCounter.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get()));
        log.debug("[AnalyticsService] getRequestCountsByRoute returning: {}", counts);
        return counts;
    }

    public Map<String, Long> getRejectionCountsByRoute() {
        Map<String, Long> counts = routeRejectionCounter.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get()));
        log.debug("[AnalyticsService] getRejectionCountsByRoute returning: {}", counts);
        return counts;
    }

    public Map<String, Long> getRejectionReasonBreakdown() {
        Map<String, Long> reasons = rejectionReasonCounter.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get()));
        if (reasons.isEmpty()) {
            log.info("[AnalyticsService] getRejectionReasonBreakdown: No rejection reasons recorded. Returning empty map.");
        } else {
            log.debug("[AnalyticsService] getRejectionReasonBreakdown returning: {}", reasons);
        }
        return reasons;
    }

    /**
     * Get total request and rejection counts for a specific route
     */
    public Map<String, Long> getRouteSpecificCounts(String routeId) {
        Map<String, Long> result = new HashMap<>();
        long requests = routeRequestCounter.getOrDefault(routeId, new AtomicLong(0)).get();
        long rejections = routeRejectionCounter.getOrDefault(routeId, new AtomicLong(0)).get();

        result.put("requestCount", requests);
        result.put("rejectedCount", rejections);

        log.info("[AnalyticsService] getRouteSpecificCounts for routeId '{}': Requests={}, Rejections={}",
                routeId, requests, rejections);

        // DEBUG: Log all available route IDs in our counters
        log.info("[AnalyticsService] Available route IDs in requestCounter: {}", routeRequestCounter.keySet());
        log.info("[AnalyticsService] Available route IDs in rejectionCounter: {}", routeRejectionCounter.keySet());

        // DEBUG: Check for similar route IDs (case-insensitive, whitespace)
        String normalizedSearchId = routeId.trim().toLowerCase();
        log.info("[AnalyticsService] Searching for normalized routeId: '{}'", normalizedSearchId);

        for (String availableRouteId : routeRequestCounter.keySet()) {
            String normalizedAvailable = availableRouteId.trim().toLowerCase();
            log.info("[AnalyticsService] Comparing with available routeId: '{}' (normalized: '{}')",
                    availableRouteId, normalizedAvailable);
            if (normalizedAvailable.equals(normalizedSearchId)) {
                log.info("[AnalyticsService] Found case/whitespace mismatch! Requested: '{}', Available: '{}'",
                        routeId, availableRouteId);
            }
        }

        return result;
    }

    /**
     * Get minute-level metrics for a specific route
     */
    public Map<String, Long> getRouteSpecificMinuteMetrics(String routeId) {
        long now = System.currentTimeMillis();
        long currentMinuteEpoch = now / 60000;
        long previousMinuteEpoch = currentMinuteEpoch - 1;

        Map<String, Long> currentMinuteRequests = timeSeriesRequests.getOrDefault(currentMinuteEpoch, Collections.emptyMap());
        Map<String, Long> previousMinuteRequests = timeSeriesRequests.getOrDefault(previousMinuteEpoch, Collections.emptyMap());
        Map<String, Long> currentMinuteRejections = timeSeriesRejections.getOrDefault(currentMinuteEpoch, Collections.emptyMap());
        Map<String, Long> previousMinuteRejections = timeSeriesRejections.getOrDefault(previousMinuteEpoch, Collections.emptyMap());

        long currentReq = currentMinuteRequests.getOrDefault(routeId, 0L);
        long previousReq = previousMinuteRequests.getOrDefault(routeId, 0L);
        long currentRej = currentMinuteRejections.getOrDefault(routeId, 0L);
        long previousRej = previousMinuteRejections.getOrDefault(routeId, 0L);

        Map<String, Long> result = new HashMap<>();
        result.put("requestsCurrentMinute", currentReq);
        result.put("requestsPreviousMinute", previousReq);
        result.put("rejectedCurrentMinute", currentRej);
        result.put("rejectedPreviousMinute", previousRej);

        log.debug("[AnalyticsService] getRouteSpecificMinuteMetrics for routeId '{}': CurrentReq={}, PrevReq={}, CurrentRej={}, PrevRej={}",
                routeId, currentReq, previousReq, currentRej, previousRej);
        return result;
    }

    /**
     * Get rejection reasons breakdown for a specific route
     * Note: Since rejection reasons are stored globally, we need to implement route-specific tracking
     * For now, this returns the global breakdown but could be enhanced to track per-route rejection reasons
     */
    public Map<String, Long> getRouteSpecificRejectionReasons(String routeId) {
        // TODO: Implement per-route rejection reason tracking if needed
        // For now, we'll return proportional data based on route's rejection percentage
        long routeRejections = routeRejectionCounter.getOrDefault(routeId, new AtomicLong(0)).get();
        long totalRejections = routeRejectionCounter.values().stream().mapToLong(AtomicLong::get).sum();

        if (routeRejections == 0 || totalRejections == 0) {
            log.debug("[AnalyticsService] getRouteSpecificRejectionReasons for routeId '{}': No rejections found", routeId);
            return Collections.emptyMap();
        }

        // Calculate route's proportion of total rejections
        double routeProportion = (double) routeRejections / totalRejections;

        Map<String, Long> globalReasons = getRejectionReasonBreakdown();
        Map<String, Long> routeSpecificReasons = new HashMap<>();

        globalReasons.forEach((reason, count) -> {
            long routeReasonCount = Math.round(count * routeProportion);
            if (routeReasonCount > 0) {
                routeSpecificReasons.put(reason, routeReasonCount);
            }
        });

        log.debug("[AnalyticsService] getRouteSpecificRejectionReasons for routeId '{}': {} reasons with total {} rejections",
                routeId, routeSpecificReasons.size(), routeRejections);
        return routeSpecificReasons;
    }

    public double getAverageResponseTime(String routeId) {
        List<Long> times = routeResponseTimes.get(routeId);
        if (times == null || times.isEmpty()) {
            log.debug("[AnalyticsService] getAverageResponseTime: No response times recorded for routeId: '{}'. Returning 0.", routeId);
            return 0.0; // Return 0 if no data
        }

        double average;
        synchronized (times) { // Ensure thread-safety for stream operation
            average = times.stream().mapToLong(t -> t).average().orElse(0.0);
        }
        log.debug("[AnalyticsService] getAverageResponseTime for routeId '{}': {} ms (from {} samples)", routeId, average, times.size());
        return average;
    }

    public List<Map<String, Object>> getTimeSeriesData(int minutes, String requestedRouteId) {
        log.info("[AnalyticsService] getTimeSeriesData called. Minutes: {}, Requested routeId: '{}'", minutes, requestedRouteId);
        List<Map<String, Object>> result = new ArrayList<>();
        long nowMinuteEpoch = System.currentTimeMillis() / 60000;

        // Log current data state for debugging
        if (timeSeriesRequests.isEmpty() && timeSeriesRejections.isEmpty()) {
            log.info("[AnalyticsService] No time series data available. Returning empty result.");
        } else if (log.isDebugEnabled()) {
            // Log a small part of the stored data to verify keys if not empty
            timeSeriesRequests.entrySet().stream().findFirst().ifPresent(entry -> {
                try {
                    log.debug("[AnalyticsService] Current keys in timeSeriesRequests for first available timestamp {}: {}",
                            entry.getKey(), objectMapper.writeValueAsString(entry.getValue().keySet()));
                } catch (JsonProcessingException e) {
                    log.warn("Error logging timeSeriesRequests keys");
                }
            });
        }

        DateTimeFormatter formatter;
        if (minutes <= 60) formatter = DateTimeFormatter.ofPattern("HH:mm");
        else if (minutes <= 24 * 60) formatter = DateTimeFormatter.ofPattern("HH:00");
        else formatter = DateTimeFormatter.ofPattern("MM-dd");

        int interval = 1;
        if (minutes > 60 && minutes <= 24 * 60) interval = 60;
        else if (minutes > 24 * 60) interval = 24 * 60;

        for (long currentTimestampMinuteEpoch = nowMinuteEpoch - minutes; currentTimestampMinuteEpoch <= nowMinuteEpoch; currentTimestampMinuteEpoch += interval) {
            // Ensure we only add points at the desired interval alignment
            if ((currentTimestampMinuteEpoch - (nowMinuteEpoch - minutes)) % interval == 0) {
                Map<String, Object> point = new HashMap<>();
                LocalDateTime dateTime = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(currentTimestampMinuteEpoch * 60000), ZoneId.systemDefault());
                point.put("time", dateTime.format(formatter));

                Map<String, Long> requestsAtTimestamp = timeSeriesRequests.getOrDefault(currentTimestampMinuteEpoch, Collections.emptyMap());
                Map<String, Long> rejectionsAtTimestamp = timeSeriesRejections.getOrDefault(currentTimestampMinuteEpoch, Collections.emptyMap());

                if (requestedRouteId != null && !requestedRouteId.equalsIgnoreCase("all")) {
                    log.debug("[AnalyticsService] Filtering time series for specific routeId: '{}' at timestampEpoch: {}. Available request keys: {}, rejection keys: {}",
                            requestedRouteId, currentTimestampMinuteEpoch, requestsAtTimestamp.keySet(), rejectionsAtTimestamp.keySet());
                    long reqCount = requestsAtTimestamp.getOrDefault(requestedRouteId, 0L);
                    long rejCount = rejectionsAtTimestamp.getOrDefault(requestedRouteId, 0L);
                    point.put("requests", reqCount);
                    point.put("rejected", rejCount);
                    point.put("accepted", Math.max(0, reqCount - rejCount));
                    // Get actual average response time for this route
                    point.put("avgResponseTime", getAverageResponseTime(requestedRouteId));
                    log.debug("[AnalyticsService] Data for specific routeId '{}' at {}: Requests={}, Rejected={}, Accepted={}",
                            requestedRouteId, point.get("time"), reqCount, rejCount, point.get("accepted"));
                } else { // "all" routes or routeId is null
                    log.debug("[AnalyticsService] Aggregating for 'all' routes at timestampEpoch: {}. Request keys: {}, Rejection keys: {}",
                            currentTimestampMinuteEpoch, requestsAtTimestamp.keySet(), rejectionsAtTimestamp.keySet());
                    long totalRequests = requestsAtTimestamp.values().stream().mapToLong(Long::longValue).sum();
                    long totalRejections = rejectionsAtTimestamp.values().stream().mapToLong(Long::longValue).sum();
                    point.put("requests", totalRequests);
                    point.put("rejected", totalRejections);
                    point.put("accepted", Math.max(0, totalRequests - totalRejections));
                    // For "all" routes, calculate weighted average of response times
                    double avgAllResponseTime = routeResponseTimes.keySet().stream()
                            .mapToDouble(this::getAverageResponseTime)
                            .filter(time -> time > 0) // Only include routes with actual data
                            .average().orElse(0.0);
                    point.put("avgResponseTime", avgAllResponseTime);
                    log.debug("[AnalyticsService] Data for 'all' routes at {}: Requests={}, Rejected={}, Accepted={}",
                            point.get("time"), totalRequests, totalRejections, point.get("accepted"));
                }
                result.add(point);
            }
        }
        log.info("[AnalyticsService] getTimeSeriesData returning {} points for requestedRouteId: '{}'", result.size(), requestedRouteId);
        return result;
    }

    public void cleanupOldData() {
        long cutoffMinuteEpoch = (System.currentTimeMillis() / 60000) - (7 * 24 * 60); // 7 days ago
        log.info("[AnalyticsService] cleanupOldData called. Cutoff timestamp (minute epoch): {}", cutoffMinuteEpoch);

        int beforeReqSize = timeSeriesRequests.size();
        timeSeriesRequests.keySet().removeIf(timestamp -> timestamp < cutoffMinuteEpoch);
        log.info("[AnalyticsService] Cleaned timeSeriesRequests. Before: {}, After: {}. Removed: {}",
                beforeReqSize, timeSeriesRequests.size(), beforeReqSize - timeSeriesRequests.size());

        int beforeRejSize = timeSeriesRejections.size();
        timeSeriesRejections.keySet().removeIf(timestamp -> timestamp < cutoffMinuteEpoch);
        log.info("[AnalyticsService] Cleaned timeSeriesRejections. Before: {}, After: {}. Removed: {}",
                beforeRejSize, timeSeriesRejections.size(), beforeRejSize - timeSeriesRejections.size());

        routeResponseTimes.forEach((routeId, times) -> {
            synchronized (times) {
                if (times.size() > 1000) { // Keep this trimming logic for response times
                    int oldSize = times.size();
                    times.subList(0, oldSize - 1000).clear();
                    log.debug("[AnalyticsService] Trimmed routeResponseTimes for routeId '{}'. Old size: {}, New size: {}",
                            routeId, oldSize, times.size());
                }
            }
        });
    }
}