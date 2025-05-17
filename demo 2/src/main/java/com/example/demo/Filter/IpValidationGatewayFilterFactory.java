package com.example.demo.Filter;

import com.example.demo.Db.IpUtils;
import com.example.demo.Entity.AllowedIp;
import com.example.demo.Entity.GatewayRoute;
import com.example.demo.Repository.GatewayRouteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
public class IpValidationGatewayFilterFactory extends AbstractGatewayFilterFactory<Void> {

    private final GatewayRouteRepository gatewayRouteRepository;

    @Autowired
    public IpValidationGatewayFilterFactory(GatewayRouteRepository gatewayRouteRepository) {
        super(Void.class);
        this.gatewayRouteRepository = gatewayRouteRepository;
    }

    @Override
    public GatewayFilter apply(Void unused) {
        return (exchange, chain) -> {
            String requestPath = exchange.getRequest().getURI().getPath();
            log.info("IP Validation Filter: requestPath={}", requestPath);

            // Extract client IP early to make debugging clearer
            ServerHttpRequest request = exchange.getRequest();
            String clientIp = IpUtils.getClientIp(request);
            log.info("Client IP extracted: {}", clientIp);

            // Load all routes from the database with eager fetching
            List<GatewayRoute> allRoutes = gatewayRouteRepository.findAllWithAllowedIpsAndRateLimit();
            log.debug("All routes loaded from DB ({} total)", allRoutes.size());

            // For debugging - output all routes and their allowed IPs
            for (GatewayRoute route : allRoutes) {
                log.debug("Route ID: {}, Predicate: {}, WithIpFilter: {}",
                        route.getId(), route.getPredicates(), route.getWithIpFilter());
                if (route.getAllowedIps() != null) {
                    for (AllowedIp ip : route.getAllowedIps()) {
                        log.debug("  - Allowed IP for route {}: {}", route.getId(), ip.getIp());
                    }
                }
            }

            // Use AntPathMatcher to properly compare request path with stored predicate patterns
            AntPathMatcher matcher = new AntPathMatcher();
            GatewayRoute matchingRoute = null;
            for (GatewayRoute route : allRoutes) {
                String predicate = route.getPredicates();
                log.debug("Checking if path '{}' matches predicate '{}'", requestPath, predicate);
                if (matcher.match(predicate, requestPath)) {
                    matchingRoute = route;
                    log.info("Found matching route: ID={}, Predicate={}", route.getId(), predicate);
                    break;
                }
            }

            if (matchingRoute == null) {
                log.warn("No matching route pattern found for path: {}", requestPath);
                exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
                return exchange.getResponse().setComplete();
            }

            // Check if IP filtering is enabled for this route
            if (matchingRoute.getWithIpFilter() == null || !matchingRoute.getWithIpFilter()) {
                log.info("IP filtering is disabled for route ID={}. Passing request along.", matchingRoute.getId());
                return chain.filter(exchange);
            }

            // Get the allowed IPs specifically for this matching route
            List<AllowedIp> allowedIpsForRoute = matchingRoute.getAllowedIps();

            log.info("Route ID {} has IP filtering enabled. Checking IP {} against {} allowed IPs",
                    matchingRoute.getId(), clientIp,
                    allowedIpsForRoute != null ? allowedIpsForRoute.size() : 0);

            // Check if there are any allowed IPs for this route
            if (allowedIpsForRoute == null || allowedIpsForRoute.isEmpty()) {
                log.error("No allowed IPs set for route ID={}. Returning 403.", matchingRoute.getId());
                exchange.getAttributes().put("ipFilterRejection", true); // Attribute set
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            // Log all allowed IPs for this route
            log.info("Allowed IPs for route ID={}", matchingRoute.getId());
            for (AllowedIp ip : allowedIpsForRoute) {
                log.info("  - Allowed IP: '{}'", ip.getIp());
            }

            // Compare the client IP against the allowed IPs for this specific route
            boolean isAllowed = false;
            for (AllowedIp ipEntity : allowedIpsForRoute) {
                if (ipEntity != null && ipEntity.getIp() != null) {
                    String allowedIp = ipEntity.getIp().trim();
                    log.debug("Comparing client IP '{}' with allowed IP '{}'", clientIp, allowedIp);
                    if (clientIp.equals(allowedIp)) {
                        isAllowed = true;
                        log.info("MATCH FOUND: Client IP '{}' matches allowed IP '{}'", clientIp, allowedIp);
                        break;
                    }
                }
            }

            if (isAllowed) {
                log.info("IP {} is ALLOWED for route ID={}. Proceeding with request.", clientIp, matchingRoute.getId());
                return chain.filter(exchange);
            } else {
                log.warn("ACCESS DENIED for IP {} on route ID={}. IP not in allowed list.", clientIp, matchingRoute.getId());
                exchange.getAttributes().put("ipFilterRejection", true); // Attribute set
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }
        };
    }
}