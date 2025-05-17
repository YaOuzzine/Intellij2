package com.example.demo.Filter;

import com.example.demo.Entity.GatewayRoute;
import com.example.demo.Repository.GatewayRouteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

@Slf4j
@Component
public class TokenValidationGatewayFilterFactory extends AbstractGatewayFilterFactory<Void> {

    private final GatewayRouteRepository gatewayRouteRepository;
    private final ReactiveJwtDecoder jwtDecoder;

    @Autowired
    public TokenValidationGatewayFilterFactory(
            GatewayRouteRepository gatewayRouteRepository,
            ReactiveJwtDecoder jwtDecoder
    ) {
        super(Void.class);
        this.gatewayRouteRepository = gatewayRouteRepository;
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public GatewayFilter apply(Void unused) {
        return (exchange, chain) -> {
            String requestPath = exchange.getRequest().getURI().getPath();
            log.info("Token Validation Filter: requestPath={}", requestPath);

            // First, get the route from the exchange
            Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            if (route == null) {
                log.warn("No route found in exchange attributes for path: {}", requestPath);
                return chain.filter(exchange);
            }

            // Get the route ID
            String routeId = route.getId();
            log.info("Processing token validation for route: {}", routeId);

            // Check direct database for current withToken value
            return Mono.fromCallable(() -> {
                        // This runs in a separate thread to avoid blocking the event loop
                        Boolean withTokenMetadata = null;

                        // First try to get from metadata (this is faster)
                        Map<String, Object> metadata = route.getMetadata();
                        if (metadata != null && metadata.containsKey("withToken")) {
                            withTokenMetadata = (Boolean) metadata.get("withToken");
                            log.info("Route metadata indicates withToken={} for route {}", withTokenMetadata, routeId);
                        }

                        // If metadata doesn't have the value or to double-check, query the database
                        GatewayRoute dbRoute = null;
                        try {
                            // Extract numeric ID if route ID is in format "route-X"
                            Long numericId = null;
                            if (routeId.startsWith("route-")) {
                                try {
                                    numericId = Long.parseLong(routeId.substring(6));
                                } catch (NumberFormatException e) {
                                    // Not a numeric ID, will try to find by routeId
                                }
                            }

                            // Try to find route in database
                            if (numericId != null) {
                                dbRoute = gatewayRouteRepository.findById(numericId).orElse(null);
                            } else {
                                dbRoute = gatewayRouteRepository.findByRouteId(routeId);
                            }

                            if (dbRoute != null) {
                                Boolean dbWithToken = dbRoute.getWithToken();
                                log.info("Database value for withToken={} for route {}", dbWithToken, routeId);

                                // If metadata and DB values don't match, log a warning
                                if (withTokenMetadata != null && !withTokenMetadata.equals(dbWithToken)) {
                                    log.warn("Metadata withToken={} doesn't match database withToken={} for route {}. Using database value.",
                                            withTokenMetadata, dbWithToken, routeId);
                                }

                                // Always prefer the database value
                                return dbWithToken;
                            } else {
                                log.warn("Could not find route {} in database", routeId);
                            }
                        } catch (Exception e) {
                            log.error("Error querying database for route {}: {}", routeId, e.getMessage(), e);
                        }

                        // If we couldn't get from DB, fall back to metadata
                        return withTokenMetadata;
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .defaultIfEmpty(Boolean.FALSE) // Default to NOT requiring token if we can't determine
                    .flatMap(withToken -> {
                        if (!Boolean.TRUE.equals(withToken)) {
                            log.info("Token validation SKIPPED for path {} (withToken={})", requestPath, withToken);
                            return chain.filter(exchange);
                        }

                        log.info("Token validation REQUIRED for path {} (withToken=true)", requestPath);

                        // Validate token
                        HttpHeaders headers = exchange.getRequest().getHeaders();
                        String authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);

                        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                            log.warn("Missing or invalid Authorization header for path: {}", requestPath);
                            exchange.getAttributes().put("tokenValidationRejection", true); // Attribute set
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        }

                        String token = authHeader.substring(7);
                        log.info("Validating token for path: {}", requestPath);

                        return Mono.just(token)
                                .flatMap(jwtDecoder::decode)
                                .flatMap(jwt -> {
                                    log.info("Token is valid for path: {}", requestPath);
                                    return chain.filter(exchange);
                                })
                                .onErrorResume(e -> {
                                    log.warn("Token validation failed for path {}: {}", requestPath, e.getMessage());
                                    exchange.getAttributes().put("tokenValidationRejection", true); // Attribute set
                                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                                    return exchange.getResponse().setComplete();
                                });
                    });
        };
    }
}