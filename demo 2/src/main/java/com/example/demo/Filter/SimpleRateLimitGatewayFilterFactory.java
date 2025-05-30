package com.example.demo.Filter;

import com.example.demo.Db.IpUtils;
import com.example.demo.Entity.GatewayRoute;
import com.example.demo.Entity.RateLimit;
import com.example.demo.Repository.GatewayRouteRepository;
import com.example.demo.Service.AnalyticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SimpleRateLimitGatewayFilterFactory extends AbstractGatewayFilterFactory<Void> {

    private final GatewayRouteRepository gatewayRouteRepository;
    private final AnalyticsService analyticsService;

    // In-memory tracker for client requests
    private final Map<String, RequestTracker> requestMap = new ConcurrentHashMap<>();

    @Autowired
    public SimpleRateLimitGatewayFilterFactory(GatewayRouteRepository gatewayRouteRepository,
                                               AnalyticsService analyticsService) {
        super(Void.class);
        this.gatewayRouteRepository = gatewayRouteRepository;
        this.analyticsService = analyticsService;
    }

    @Override
    public GatewayFilter apply(Void unused) {
        return (exchange, chain) -> {
            // 1) Extract the request path
            String requestPath = exchange.getRequest().getURI().getPath();
            log.info("Rate Limiting Filter: requestPath={}", requestPath);

            // 2) Load all routes from the database with their allowed IPs and RateLimit
            List<GatewayRoute> allRoutes = gatewayRouteRepository.findAllWithAllowedIpsAndRateLimit();
            if (allRoutes.isEmpty()) {
                log.error("No routes found in the database.");
                exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
                return exchange.getResponse().setComplete();
            }

            // 3) Sort routes by specificity (longest prefix first)
            allRoutes.sort(Comparator.comparingInt(
                    r -> -r.getPredicates().replace("/**", "").length()));

            // 4) Find the best matching route
            GatewayRoute matchingRoute = null;
            for (GatewayRoute route : allRoutes) {
                String routePredicate = route.getPredicates();
                // Remove trailing "/**" if present
                if (routePredicate.endsWith("/**")) {
                    routePredicate = routePredicate.substring(0, routePredicate.length() - 3);
                }
                if (requestPath.startsWith(routePredicate)) {
                    matchingRoute = route;
                    break;
                }
            }

            // 5) If no matching route is found, return 404
            if (matchingRoute == null) {
                log.warn("No matching route found for path={}", requestPath);
                exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
                return exchange.getResponse().setComplete();
            }

            String routeId = matchingRoute.getRouteId() != null ?
                    matchingRoute.getRouteId() : "route-" + matchingRoute.getId();

            log.info("Matching route found: {}", routeId);

            // Record the request in analytics service
            try {
                analyticsService.recordRequest(routeId);
                log.debug("Recorded request in analytics for route: {}", routeId);
            } catch (Exception e) {
                log.warn("Failed to record request in analytics: {}", e.getMessage());
            }

            // Skip rate limiting if not enabled for this route
            if (matchingRoute.getWithRateLimit() == null || !matchingRoute.getWithRateLimit()) {
                log.info("Rate limiting disabled for route {}. Proceeding.", routeId);
                return chain.filter(exchange);
            }

            // 6) Retrieve the RateLimit object from the route
            RateLimit rl = matchingRoute.getRateLimit();
            long timeWindowMs;
            int maxRequests;
            if (rl != null && rl.getMaxRequests() != null && rl.getTimeWindowMs() != null) {
                maxRequests = rl.getMaxRequests();
                timeWindowMs = rl.getTimeWindowMs();
            } else {
                log.warn("No RateLimit entity or missing fields for route {} => using defaults.",
                        routeId);
                maxRequests = 10;
                timeWindowMs = 60000;
            }

            // 7) Build a unique key from the client IP + routeId
            String clientIp = IpUtils.getClientIp(exchange.getRequest());
            String key = clientIp + "_" + routeId;

            // 8) Retrieve or create a RequestTracker for this key
            RequestTracker tracker = requestMap.computeIfAbsent(key, k -> new RequestTracker());

            synchronized (tracker) {
                long now = Instant.now().toEpochMilli();
                // Reset counter if the time window has expired
                if ((now - tracker.getWindowStart()) > timeWindowMs) {
                    tracker.setWindowStart(now);
                    tracker.setRequestCount(0);
                }
                // Check if the client is within allowed limits
                if (tracker.getRequestCount() < maxRequests) {
                    tracker.incrementRequestCount();
                    log.info("Request allowed. clientIp={}, routeId={}, count={}",
                            clientIp, routeId, tracker.getRequestCount());
                } else {
                    log.warn("Rate limit exceeded for clientIp={} on routeId={}",
                            clientIp, routeId);

                    // Record the rejection in analytics service
                    try {
                        analyticsService.recordRejection(routeId, "Rate Limit");
                        log.debug("Recorded rate limit rejection in analytics for route: {}", routeId);
                    } catch (Exception e) {
                        log.warn("Failed to record rejection in analytics: {}", e.getMessage());
                    }

                    exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                    return exchange.getResponse().setComplete();
                }
            }

            // 9) Pass the request along if under the limit
            return chain.filter(exchange);
        };
    }

    /**
     * Internal class to track requests for a specific client within a time window.
     */
    private static class RequestTracker {
        private long windowStart = Instant.now().toEpochMilli();
        private int requestCount = 0;

        public long getWindowStart() {
            return windowStart;
        }
        public void setWindowStart(long windowStart) {
            this.windowStart = windowStart;
        }
        public int getRequestCount() {
            return requestCount;
        }
        public void setRequestCount(int requestCount) {
            this.requestCount = requestCount;
        }
        public void incrementRequestCount() {
            this.requestCount++;
        }
    }
}