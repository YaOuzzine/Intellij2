// demo 2/src/main/java/com/example/demo/Service/AnalyticsService.java
package com.example.demo.Service;

import com.example.demo.Filter.RequestCountFilter;
import com.example.demo.Repository.GatewayRouteRepository; // Added for sample data
import com.fasterxml.jackson.core.JsonProcessingException; // Added for logging
import com.fasterxml.jackson.databind.ObjectMapper; // Added for logging
import org.slf4j.Logger; // Added for logging
import org.slf4j.LoggerFactory; // Added for logging
import org.springframework.beans.factory.annotation.Autowired; // Added for constructor injection
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

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class); // Added logger

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

    // --- NEW: For improved sample data generation ---
    private final GatewayRouteRepository gatewayRouteRepositoryForSampleData;
    private final ObjectMapper objectMapper = new ObjectMapper(); // For logging complex objects
    // --- END NEW ---

    // --- MODIFIED: Constructor for injecting GatewayRouteRepository ---
    @Autowired // Ensures Spring injects the repository
    public AnalyticsService(GatewayRouteRepository gatewayRouteRepository) {
        this.gatewayRouteRepositoryForSampleData = gatewayRouteRepository;
        log.info("[AnalyticsService] Initialized. GatewayRouteRepository for sample data is {}.",
                (this.gatewayRouteRepositoryForSampleData == null ? "NOT injected" : "injected"));
    }
    // --- END MODIFIED ---


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
        // --- OLD CODE for sample rejection data ---
        /*
        if (rejectionReasonCounter.isEmpty()) {
            long totalRejected = RequestCountFilter.getTotalRejectedCount();
            if (totalRejected > 0) {
                rejectionReasonCounter.put("IP Filter", new AtomicLong((long)(totalRejected * 0.4)));
                rejectionReasonCounter.put("Token Validation", new AtomicLong((long)(totalRejected * 0.35)));
                rejectionReasonCounter.put("Rate Limit", new AtomicLong((long)(totalRejected * 0.2)));
                rejectionReasonCounter.put("Invalid Request", new AtomicLong((long)(totalRejected * 0.05)));
            }
        }
        */
        // --- END OLD CODE ---
        // New logic: Return actual data. If it's empty, it's empty. Frontend can show "No data".
        Map<String, Long> reasons = rejectionReasonCounter.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get()));
        if (reasons.isEmpty()) {
            log.warn("[AnalyticsService] getRejectionReasonBreakdown: No actual rejection reasons recorded. Returning empty map.");
        } else {
            log.debug("[AnalyticsService] getRejectionReasonBreakdown returning: {}", reasons);
        }
        return reasons;
    }

    public double getAverageResponseTime(String routeId) {
        List<Long> times = routeResponseTimes.get(routeId);
        if (times == null || times.isEmpty()) {
            // --- OLD CODE for sample response time ---
            // return 50 + Math.random() * 200;
            // --- END OLD CODE ---
            log.warn("[AnalyticsService] getAverageResponseTime: No response times recorded for routeId: '{}'. Returning 0.", routeId);
            return 0.0; // Return 0 if no data, or handle as appropriate
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

        // --- MODIFIED: Sample data population logic ---
        if (timeSeriesRequests.isEmpty() && timeSeriesRejections.isEmpty()) {
            log.warn("[AnalyticsService] No actual time series data found in timeSeriesRequests/Rejections. Populating with sample data. Requested routeId: '{}'", requestedRouteId);
            populateSampleTimeSeriesData();
        } else if (log.isDebugEnabled()) {
            // Log a small part of the stored data to verify keys if not empty
            timeSeriesRequests.entrySet().stream().findFirst().ifPresent(entry -> {
                try {
                    log.debug("[AnalyticsService] Current keys in timeSeriesRequests for first available timestamp {}: {}",
                            entry.getKey(), objectMapper.writeValueAsString(entry.getValue().keySet()));
                } catch (JsonProcessingException e) { log.warn("Error logging timeSeriesRequests keys"); }
            });
        }
        // --- END MODIFIED ---

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
                    // For avgResponseTime, you'd typically calculate it based on stored sums and counts for that minute,
                    // or fetch pre-aggregated minute-level avg response times if you store them.
                    // Here, we'll just use the overall average for simplicity in this example.
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
                    // For "all" routes, avgResponseTime could be an average of averages, or a global average.
                    // Here, we'll calculate average of all known route averages for simplicity.
                    double avgAllResponseTime = routeResponseTimes.keySet().stream()
                            .mapToDouble(this::getAverageResponseTime)
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

    // --- MODIFIED: populateSampleTimeSeriesData to use actual route IDs ---
    private void populateSampleTimeSeriesData() {
        log.info("[AnalyticsService] populateSampleTimeSeriesData called.");
        long nowMinuteEpoch = System.currentTimeMillis() / 60000;
        Random random = new Random();

        List<com.example.demo.Entity.GatewayRoute> actualGatewayRoutes = Collections.emptyList();
        if (this.gatewayRouteRepositoryForSampleData != null) {
            try {
                // Fetching from the gateway schema's repository instance
                actualGatewayRoutes = this.gatewayRouteRepositoryForSampleData.findAllWithAllowedIpsAndRateLimit();
                log.info("[AnalyticsService] Fetched {} actual routes from gateway schema for sample data generation.", actualGatewayRoutes.size());
            } catch (Exception e) {
                log.error("[AnalyticsService] Could not fetch actual routes from gateway schema for sample data: {}. Falling back to dummies.", e.getMessage(), e);
            }
        } else {
            log.warn("[AnalyticsService] GatewayRouteRepository (for sample data) is null. Using dummy routes for sample data.");
        }

        List<String> effectiveRouteIdsForSample = new ArrayList<>();
        if (!actualGatewayRoutes.isEmpty()) {
            for (com.example.demo.Entity.GatewayRoute route : actualGatewayRoutes) {
                String id = route.getRouteId() != null && !route.getRouteId().isBlank()
                        ? route.getRouteId()
                        : "route-" + route.getId(); // route.getId() is PK from gateway.gateway_routes
                effectiveRouteIdsForSample.add(id);
            }
        } else {
            log.warn("[AnalyticsService] No actual routes found or repo unavailable, using dummy route IDs for sample data.");
            effectiveRouteIdsForSample.add("sample-route-1"); // Fallback dummy ID
            effectiveRouteIdsForSample.add("sample-route-2"); // Fallback dummy ID
        }
        log.info("[AnalyticsService] Using effective route IDs for sample data: {}", effectiveRouteIdsForSample);


        RequestCountFilter.MinuteMetrics currentMetrics = RequestCountFilter.getMinuteMetrics();
        long baseCurrentRequests = currentMetrics.getRequestsCurrentMinute() > 0 ? currentMetrics.getRequestsCurrentMinute() : 50;
        long baseCurrentRejected = currentMetrics.getRejectedCurrentMinute() > 0 ? currentMetrics.getRejectedCurrentMinute() : 5;

        for (int i = 0; i < 1440; i++) { // 24 hours of data
            long timestamp = nowMinuteEpoch - i;
            double timeFactor = Math.max(0.2, 1.0 - (i / 2000.0)); // Gradual decline, minimum 20%

            long totalRequestValueForTimestamp = (long) (baseCurrentRequests * timeFactor * (0.7 + random.nextDouble() * 0.6)); // Fluctuation
            long totalRejectedValueForTimestamp = (long) (baseCurrentRejected * timeFactor * (0.5 + random.nextDouble() * 1.0));
            totalRejectedValueForTimestamp = Math.min(totalRejectedValueForTimestamp, totalRequestValueForTimestamp); // Rejections can't exceed requests

            Map<String, Long> routeRequestsAtTimestamp = new HashMap<>();
            Map<String, Long> routeRejectionsAtTimestamp = new HashMap<>();

            if (!effectiveRouteIdsForSample.isEmpty()) {
                long remainingRequests = totalRequestValueForTimestamp;
                long remainingRejections = totalRejectedValueForTimestamp;

                for (int j = 0; j < effectiveRouteIdsForSample.size(); j++) {
                    String currentSampleRouteId = effectiveRouteIdsForSample.get(j);

                    long reqShare, rejShare;
                    if (j == effectiveRouteIdsForSample.size() - 1) { // Last route gets the remainder
                        reqShare = remainingRequests;
                        rejShare = remainingRejections;
                    } else {
                        // Distribute somewhat proportionally, with randomness
                        double proportion = 1.0 / effectiveRouteIdsForSample.size();
                        reqShare = (long) (totalRequestValueForTimestamp * proportion * (0.5 + random.nextDouble()));
                        rejShare = (long) (totalRejectedValueForTimestamp * proportion * (0.5 + random.nextDouble()));

                        reqShare = Math.max(0, Math.min(reqShare, remainingRequests));
                        rejShare = Math.max(0, Math.min(rejShare, remainingRejections));
                        rejShare = Math.min(rejShare, reqShare); // Rejections for this route part <= requests for this route part
                    }
                    routeRequestsAtTimestamp.put(currentSampleRouteId, reqShare);
                    routeRejectionsAtTimestamp.put(currentSampleRouteId, rejShare);
                    remainingRequests -= reqShare;
                    remainingRejections -= rejShare;
                }
            } else { // Should not happen if fallback dummy IDs are used
                routeRequestsAtTimestamp.put("fallback_sample_route", totalRequestValueForTimestamp);
                routeRejectionsAtTimestamp.put("fallback_sample_route", totalRejectedValueForTimestamp);
            }

            if (i == 0 && log.isDebugEnabled()) { // Log for the most recent sample timestamp only
                try {
                    log.debug("[AnalyticsService] Sample data generated for timestampEpoch {}. Route requests distribution: {}",
                            timestamp, objectMapper.writeValueAsString(routeRequestsAtTimestamp));
                    log.debug("[AnalyticsService] Sample data generated for timestampEpoch {}. Route rejections distribution: {}",
                            timestamp, objectMapper.writeValueAsString(routeRejectionsAtTimestamp));
                } catch (JsonProcessingException e) {
                    log.warn("[AnalyticsService] Error serializing sample route data for logging", e);
                }
            }
            timeSeriesRequests.put(timestamp, routeRequestsAtTimestamp);
            timeSeriesRejections.put(timestamp, routeRejectionsAtTimestamp);
        }
        log.info("[AnalyticsService] Finished populating sample data for {} timestamps.", timeSeriesRequests.size());
    }
    // --- END MODIFIED ---

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