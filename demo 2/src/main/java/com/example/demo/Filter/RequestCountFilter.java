package com.example.demo.Filter;

import com.example.demo.Service.AnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Order(1) // Ensure this filter runs early (if using Spring Security).
@Component
public class RequestCountFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestCountFilter.class);
    private static final AtomicLong totalRequestCount = new AtomicLong(0);
    private static final AtomicLong totalRejectedCount = new AtomicLong(0);
    private static final ConcurrentHashMap<Long, AtomicLong> requestsPerSecond = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, AtomicLong> rejectedPerSecond = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> rejectionReasons = new ConcurrentHashMap<>();
    // Track the current minute to detect transitions.
    private static long currentMinute = System.currentTimeMillis() / 60000;

    private AnalyticsService analyticsService;

    @Autowired
    public void setAnalyticsService(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Skip metrics endpoints.
        if (path.startsWith("/api/metrics")) {
            return chain.filter(exchange);
        }

        long currentSecond = System.currentTimeMillis() / 1000;
        long newMinute = System.currentTimeMillis() / 60000;

        // Track request start time for response time calculation
        long startTime = System.currentTimeMillis();

        // Extract the routeId from the exchange attributes if available
        // Make it final to avoid the lambda capture issue
        final String routeId = (String) exchange.getAttribute("routeId") != null ?
                (String) exchange.getAttribute("routeId") : "unknown";

        // Always count this incoming request as accepted initially.
        log.debug("Counting request for path: {} with routeId: {}, second: {}", path, routeId, currentSecond);
        totalRequestCount.incrementAndGet();
        requestsPerSecond
                .computeIfAbsent(currentSecond, k -> new AtomicLong(0))
                .incrementAndGet();

        // Record in analytics service
        if (analyticsService != null) {
            analyticsService.recordRequest(routeId);
        }

        // Update current minute if needed.
        if (newMinute > currentMinute) {
            currentMinute = newMinute;
        }

        // Clean up old entries (older than 120 seconds).
        long threshold = currentSecond - 120;
        requestsPerSecond.keySet().removeIf(sec -> sec < threshold);
        rejectedPerSecond.keySet().removeIf(sec -> sec < threshold);

        // Store the analytics service reference in a final variable for lambda capture
        final AnalyticsService analyticsServiceFinal = this.analyticsService;

        // Proceed with downstream chain and then check the final status code.
        return chain.filter(exchange)
                .doFinally(signalType -> {
                    // Get the final status code
                    HttpStatusCode finalStatus = exchange.getResponse().getStatusCode();
                    // Check if the status code indicates an error (4xx or 5xx)
                    if (finalStatus != null && (finalStatus.value() >= 400 && finalStatus.value() < 600)) {
                        String reason = determineRejectionReason(finalStatus, exchange);
                        countRejectedRequest(reason, routeId);
                    }

                    // Calculate and record response time
                    long responseTime = System.currentTimeMillis() - startTime;
                    if (analyticsServiceFinal != null) {
                        analyticsServiceFinal.recordResponseTime(routeId, responseTime);
                    }
                });
    }

    /**
     * Determine the reason for rejection based on status code and exchange attributes
     */
    private String determineRejectionReason(HttpStatusCode status, ServerWebExchange exchange) {
        String reason;

        if (status.equals(HttpStatus.FORBIDDEN) &&
                exchange.getAttribute("ipFilterRejection") != null) {
            reason = "IP Filter";
        }
        else if (status.equals(HttpStatus.UNAUTHORIZED) &&
                exchange.getAttribute("tokenValidationRejection") != null) {
            reason = "Token Validation";
        }
        else if (status.equals(HttpStatus.TOO_MANY_REQUESTS)) {
            reason = "Rate Limit";
        }
        else if (status.equals(HttpStatus.BAD_REQUEST)) {
            reason = "Invalid Request";
        }
        else {
            reason = "Other (" + status.value() + ")";
        }

        return reason;
    }

    // Method to count a rejected request with a name.
    public static void countRejectedRequest(String rejectName, String routeId) {
        long currentSecond = System.currentTimeMillis() / 1000;
        log.debug("Rejected request: {} for routeId: {}, second: {}", rejectName, routeId, currentSecond);

        totalRejectedCount.incrementAndGet();
        rejectedPerSecond.computeIfAbsent(currentSecond, k -> new AtomicLong(0))
                .incrementAndGet();

        // Store rejection reason for analytics
        rejectionReasons.put("reject-" + System.nanoTime(), rejectName);

        // Access the analytics service through the ApplicationContextHolder
        // This avoids the need to reference the instance field from a static context
        try {
            AnalyticsService analyticsService = ApplicationContextHolder.getBean(AnalyticsService.class);
            if (analyticsService != null) {
                analyticsService.recordRejection(routeId, rejectName);
            }
        } catch (Exception e) {
            log.warn("Could not record rejection in AnalyticsService: {}", e.getMessage());
        }
    }

    public static long getTotalRequestCount() {
        return totalRequestCount.get();
    }

    public static long getTotalRejectedCount() {
        return totalRejectedCount.get();
    }

    public static MinuteMetrics getMinuteMetrics() {
        long now = System.currentTimeMillis();
        long currentSecond = now / 1000;
        long currentMinuteStart = (now / 60000) * 60; // Start of the current minute in seconds.
        long previousMinuteStart = currentMinuteStart - 60; // Start of the previous minute in seconds.

        long currentMinuteSum = 0;
        long previousMinuteSum = 0;
        long currentMinuteRejected = 0;
        long previousMinuteRejected = 0;

        // Count accepted and rejected requests in the current minute.
        for (long sec = currentMinuteStart; sec <= currentSecond; sec++) {
            AtomicLong count = requestsPerSecond.get(sec);
            if (count != null) {
                currentMinuteSum += count.get();
            }
            AtomicLong rejectedCount = rejectedPerSecond.get(sec);
            if (rejectedCount != null) {
                currentMinuteRejected += rejectedCount.get();
            }
        }

        // Count accepted and rejected requests in the previous minute.
        for (long sec = previousMinuteStart; sec < currentMinuteStart; sec++) {
            AtomicLong count = requestsPerSecond.get(sec);
            if (count != null) {
                previousMinuteSum += count.get();
            }
            AtomicLong rejectedCount = rejectedPerSecond.get(sec);
            if (rejectedCount != null) {
                previousMinuteRejected += rejectedCount.get();
            }
        }

        return new MinuteMetrics(currentMinuteSum, previousMinuteSum, currentMinuteRejected, previousMinuteRejected);
    }

    /**
     * Get a map of recent rejection reasons for analytics
     */
    public static ConcurrentHashMap<String, String> getRejectionReasons() {
        return rejectionReasons;
    }

    public static class MinuteMetrics {
        private final long requestsCurrentMinute;
        private final long requestsPreviousMinute;
        private final long rejectedCurrentMinute;
        private final long rejectedPreviousMinute;

        public MinuteMetrics(long current, long previous, long rejectedCurrent, long rejectedPrevious) {
            this.requestsCurrentMinute = current;
            this.requestsPreviousMinute = previous;
            this.rejectedCurrentMinute = rejectedCurrent;
            this.rejectedPreviousMinute = rejectedPrevious;
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
}
