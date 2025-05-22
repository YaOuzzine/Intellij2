// demo 2/src/main/java/com/example/demo/Filter/RequestCountFilter.java
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

// Required imports for the fix
import org.springframework.cloud.gateway.route.Route; // Added for the fix
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils; // Added for the fix

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Order(1000) // CHANGED: Run much later to ensure gateway routing is complete
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
        log.debug("[RequestCountFilter] Processing request for path: {}", path);

        // Skip metrics endpoints.
        if (path.startsWith("/api/metrics")) {
            log.trace("[RequestCountFilter] Skipping metrics endpoint: {}", path);
            return chain.filter(exchange);
        }

        long currentSecond = System.currentTimeMillis() / 1000;
        long newMinute = System.currentTimeMillis() / 60000;

        // Track request start time for response time calculation
        long startTime = System.currentTimeMillis();

        // --- IMPROVED CODE for routeId determination ---
        // Spring Cloud Gateway sets the matched Route object in the exchange attributes.
        Route gatewayRoute = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);

        final String routeId;
        if (gatewayRoute != null) {
            routeId = gatewayRoute.getId(); // This ID is set in DynamicRouteConfig
            log.info("[RequestCountFilter] ✅ Successfully matched gateway route ID: '{}' for path: {}", routeId, path);
            log.info("[RequestCountFilter] ✅ Route URI: {}", gatewayRoute.getUri());
            log.info("[RequestCountFilter] ✅ Route metadata: {}", gatewayRoute.getMetadata());
        } else {
            // This indicates the request didn't match any gateway route, or we're running too early
            routeId = "UNMATCHED_GATEWAY_REQUEST";
            log.warn("[RequestCountFilter] ❌ No GATEWAY_ROUTE_ATTR found in exchange for path: {}. This might indicate:", path);
            log.warn("[RequestCountFilter] 1. Request doesn't match any gateway route pattern");
            log.warn("[RequestCountFilter] 2. Filter is running before gateway routing (check @Order)");
            log.warn("[RequestCountFilter] 3. Gateway route configuration issue");
            log.warn("[RequestCountFilter] 4. Request not going through gateway (wrong port?)");

            // Debug: Log all exchange attributes to see what's available
            log.warn("[RequestCountFilter] Available exchange attributes: {}", exchange.getAttributes().keySet());

            // Debug: Log request details
            log.warn("[RequestCountFilter] Request method: {}", exchange.getRequest().getMethod());
            log.warn("[RequestCountFilter] Request path: {}", exchange.getRequest().getURI().getPath());
            log.warn("[RequestCountFilter] Request host: {}", exchange.getRequest().getURI().getHost());
            log.warn("[RequestCountFilter] Request port: {}", exchange.getRequest().getURI().getPort());
            log.warn("[RequestCountFilter] Request headers: {}", exchange.getRequest().getHeaders().toSingleValueMap());
        }
        // --- END OF IMPROVED CODE ---

        // Always count this incoming request as accepted initially.
        log.debug("[RequestCountFilter] Counting request for path: {} with resolved routeId: '{}', currentSecond: {}", path, routeId, currentSecond);
        totalRequestCount.incrementAndGet();
        requestsPerSecond
                .computeIfAbsent(currentSecond, k -> new AtomicLong(0))
                .incrementAndGet();

        // Record in analytics service
        if (analyticsService != null) {
            log.debug("[RequestCountFilter] Recording request in AnalyticsService for routeId: '{}'", routeId);
            analyticsService.recordRequest(routeId); // Pass the correctly resolved routeId
        } else {
            log.warn("[RequestCountFilter] AnalyticsService is null. Cannot record request for routeId: '{}'", routeId);
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
                    log.trace("[RequestCountFilter] doFinally triggered for routeId: '{}', signalType: {}", routeId, signalType);
                    // Get the final status code
                    HttpStatusCode finalStatus = exchange.getResponse().getStatusCode();
                    log.debug("[RequestCountFilter] Final status for routeId '{}': {}", routeId, finalStatus);

                    // Check if the status code indicates an error (4xx or 5xx)
                    if (finalStatus != null && (finalStatus.value() >= 400 && finalStatus.value() < 600)) {
                        String reason = determineRejectionReason(finalStatus, exchange);
                        log.info("[RequestCountFilter] Request for routeId '{}' resulted in status {} ({}). Rejection reason: {}", routeId, finalStatus.value(), finalStatus.isError(), reason);
                        countRejectedRequest(reason, routeId); // Pass the correctly resolved routeId
                    }

                    // Calculate and record response time
                    long responseTime = System.currentTimeMillis() - startTime;
                    log.debug("[RequestCountFilter] Response time for routeId '{}': {} ms", routeId, responseTime);
                    if (analyticsServiceFinal != null) {
                        analyticsServiceFinal.recordResponseTime(routeId, responseTime); // Pass the correctly resolved routeId
                    } else {
                        log.warn("[RequestCountFilter] AnalyticsService (final) is null. Cannot record response time for routeId: '{}'", routeId);
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
        log.trace("[RequestCountFilter] Determined rejection reason: '{}' for status: {}", reason, status);
        return reason;
    }

    // Method to count a rejected request with a name.
    public static void countRejectedRequest(String rejectName, String routeId) {
        long currentSecond = System.currentTimeMillis() / 1000;
        log.debug("[RequestCountFilter] Counting rejected request: '{}' for routeId: '{}', second: {}", rejectName, routeId, currentSecond);

        totalRejectedCount.incrementAndGet();
        rejectedPerSecond.computeIfAbsent(currentSecond, k -> new AtomicLong(0))
                .incrementAndGet();

        // Store rejection reason for analytics
        rejectionReasons.put("reject-" + System.nanoTime(), rejectName); // Key needs to be unique

        // Access the analytics service through the ApplicationContextHolder
        // This avoids the need to reference the instance field from a static context
        try {
            AnalyticsService staticAnalyticsService = ApplicationContextHolder.getBean(AnalyticsService.class);
            if (staticAnalyticsService != null) {
                log.debug("[RequestCountFilter] Recording rejection in AnalyticsService (via ApplicationContextHolder) for routeId: '{}', reason: '{}'", routeId, rejectName);
                staticAnalyticsService.recordRejection(routeId, rejectName); // Pass the correctly resolved routeId
            } else {
                log.warn("[RequestCountFilter] AnalyticsService (via ApplicationContextHolder) is null. Cannot record rejection for routeId: '{}'", routeId);
            }
        } catch (Exception e) {
            log.warn("[RequestCountFilter] Could not record rejection in AnalyticsService (via ApplicationContextHolder): {}", e.getMessage());
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
        log.trace("[RequestCountFilter] MinuteMetrics: CurrentReq={}, PrevReq={}, CurrentRej={}, PrevRej={}",
                currentMinuteSum, previousMinuteSum, currentMinuteRejected, previousMinuteRejected);
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