// demo 2/src/main/java/com/example/demo/Filter/RequestCountFilter.java
package com.example.demo.Filter;

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

@Order(10001) // CHANGED: Run after gateway routing (10000) but before actual routing filters
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
        log.info("[RequestCountFilter] ========== PROCESSING REQUEST: {} ==========", path);

        // Skip metrics endpoints that are served directly by this service
        if (path.startsWith("/api/metrics")) {
            log.trace("[RequestCountFilter] Skipping metrics endpoint: {}", path);
            return chain.filter(exchange);
        }

        // Skip actuator endpoints
        if (path.startsWith("/actuator")) {
            log.trace("[RequestCountFilter] Skipping actuator endpoint: {}", path);
            return chain.filter(exchange);
        }

        long currentSecond = System.currentTimeMillis() / 1000;
        long newMinute = System.currentTimeMillis() / 60000;

        // Track request start time for response time calculation
        long startTime = System.currentTimeMillis();

        // DETAILED LOGGING: Check all exchange attributes to see what's available
        log.info("[RequestCountFilter] 🔍 ALL EXCHANGE ATTRIBUTES:");
        exchange.getAttributes().forEach((key, value) -> {
            log.info("[RequestCountFilter] 🔍   {} = {}", key, value);
        });

        // Try to get the gateway route - this will only be present for requests going through gateway routing
        Route gatewayRoute = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);

        log.info("[RequestCountFilter] 🔍 GATEWAY_ROUTE_ATTR lookup result: {}", gatewayRoute);
        if (gatewayRoute != null) {
            log.info("[RequestCountFilter] 🔍 Gateway Route Details:");
            log.info("[RequestCountFilter] 🔍   Route ID: '{}'", gatewayRoute.getId());
            log.info("[RequestCountFilter] 🔍   Route URI: {}", gatewayRoute.getUri());
            log.info("[RequestCountFilter] 🔍   Route Predicate: {}", gatewayRoute.getPredicate());
            log.info("[RequestCountFilter] 🔍   Route Metadata: {}", gatewayRoute.getMetadata());
            log.info("[RequestCountFilter] 🔍   Route Filters: {}", gatewayRoute.getFilters().size());
        } else {
            log.error("[RequestCountFilter] 🔍 ❌ GATEWAY_ROUTE_ATTR IS NULL for path: {}", path);
        }

        final String routeId;
        final boolean isGatewayRequest;

        if (gatewayRoute != null) {
            // This is a request that matched a gateway route pattern
            routeId = gatewayRoute.getId();
            isGatewayRequest = true;
            log.info("[RequestCountFilter] ✅ Gateway-routed request - Route ID: '{}', Path: {}", routeId, path);
            log.info("[RequestCountFilter] ✅ Route URI: {}", gatewayRoute.getUri());
            log.info("[RequestCountFilter] ✅ Route metadata: {}", gatewayRoute.getMetadata());
        } else {
            // This request didn't go through gateway routing
            // For debugging purposes, let's be more permissive and still record some requests
            if (path.startsWith("/server-final")) {
                // This should have been gateway-routed! This is a problem.
                routeId = "MISSING_GATEWAY_ROUTE_" + path.split("/")[1]; // e.g., "MISSING_GATEWAY_ROUTE_server-final"
                isGatewayRequest = true; // Treat as gateway request for analytics
                log.error("[RequestCountFilter] ❌ CRITICAL: Path '{}' should be gateway-routed but GATEWAY_ROUTE_ATTR is missing!", path);
                log.error("[RequestCountFilter] ❌ Check gateway routing configuration and filter order");
                log.error("[RequestCountFilter] ❌ Available exchange attributes: {}", exchange.getAttributes().keySet());
            } else if (path.startsWith("/api/metrics") || path.startsWith("/api/auth")) {
                // These are legitimate direct API calls
                routeId = "direct-api-call";
                isGatewayRequest = false;
                log.trace("[RequestCountFilter] Direct API call to: {}", path);
            } else {
                // Other requests (favicon, actuator, etc.)
                routeId = "other-request";
                isGatewayRequest = false;
                log.trace("[RequestCountFilter] Other request to: {}", path);
            }
        }

        // Always count this incoming request in global counters
        log.debug("[RequestCountFilter] Counting request for path: {} with resolved routeId: '{}', currentSecond: {}", path, routeId, currentSecond);
        totalRequestCount.incrementAndGet();
        requestsPerSecond
                .computeIfAbsent(currentSecond, k -> new AtomicLong(0))
                .incrementAndGet();

        // Only record in analytics service for gateway-routed requests to maintain accurate route-specific metrics
        if (analyticsService != null && isGatewayRequest) {
            log.debug("[RequestCountFilter] Recording gateway request in AnalyticsService for routeId: '{}'", routeId);
            analyticsService.recordRequest(routeId);
        } else if (analyticsService == null) {
            log.warn("[RequestCountFilter] AnalyticsService is null. Cannot record request for routeId: '{}'", routeId);
        } else {
            log.trace("[RequestCountFilter] Skipping analytics recording for non-gateway request: {}", path);
        }

        // Update current minute if needed.
        if (newMinute > currentMinute) {
            currentMinute = newMinute;
        }

        // Clean up old entries (older than 120 seconds).
        long threshold = currentSecond - 120;
        requestsPerSecond.keySet().removeIf(sec -> sec < threshold);
        rejectedPerSecond.keySet().removeIf(sec -> sec < threshold);

        // Store the analytics service reference and gateway request flag in final variables for lambda capture
        final AnalyticsService analyticsServiceFinal = this.analyticsService;
        final boolean isGatewayRequestFinal = isGatewayRequest;

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
                        countRejectedRequest(reason, routeId, isGatewayRequestFinal);
                    }

                    // Calculate and record response time only for gateway requests
                    long responseTime = System.currentTimeMillis() - startTime;
                    log.debug("[RequestCountFilter] Response time for routeId '{}': {} ms", routeId, responseTime);
                    if (analyticsServiceFinal != null && isGatewayRequestFinal) {
                        analyticsServiceFinal.recordResponseTime(routeId, responseTime);
                        log.trace("[RequestCountFilter] Recorded response time for gateway request: {} ms", responseTime);
                    } else if (analyticsServiceFinal == null) {
                        log.warn("[RequestCountFilter] AnalyticsService (final) is null. Cannot record response time for routeId: '{}'", routeId);
                    } else {
                        log.trace("[RequestCountFilter] Skipping response time recording for non-gateway request");
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
    public static void countRejectedRequest(String rejectName, String routeId, boolean isGatewayRequest) {
        long currentSecond = System.currentTimeMillis() / 1000;
        log.debug("[RequestCountFilter] Counting rejected request: '{}' for routeId: '{}', second: {}, isGateway: {}", rejectName, routeId, currentSecond, isGatewayRequest);

        // Always increment global counters
        totalRejectedCount.incrementAndGet();
        rejectedPerSecond.computeIfAbsent(currentSecond, k -> new AtomicLong(0))
                .incrementAndGet();

        // Store rejection reason for analytics
        rejectionReasons.put("reject-" + System.nanoTime(), rejectName); // Key needs to be unique

        // Only record in analytics service for gateway requests
        if (isGatewayRequest) {
            try {
                AnalyticsService staticAnalyticsService = ApplicationContextHolder.getBean(AnalyticsService.class);
                if (staticAnalyticsService != null) {
                    log.debug("[RequestCountFilter] Recording gateway rejection in AnalyticsService for routeId: '{}', reason: '{}'", routeId, rejectName);
                    staticAnalyticsService.recordRejection(routeId, rejectName);
                } else {
                    log.warn("[RequestCountFilter] AnalyticsService (via ApplicationContextHolder) is null. Cannot record rejection for routeId: '{}'", routeId);
                }
            } catch (Exception e) {
                log.warn("[RequestCountFilter] Could not record rejection in AnalyticsService: {}", e.getMessage());
            }
        } else {
            log.trace("[RequestCountFilter] Skipping analytics recording for non-gateway rejection");
        }
    }

    // Overloaded method for backward compatibility - assumes non-gateway request
    public static void countRejectedRequest(String rejectName, String routeId) {
        countRejectedRequest(rejectName, routeId, false);
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