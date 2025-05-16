package com.example.demo.Filter;

import com.example.demo.Entity.GatewayRoute;
import com.example.demo.Repository.GatewayRouteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

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

            // Load all routes from the database.
            List<GatewayRoute> allRoutes = gatewayRouteRepository.findAll();
            if (allRoutes.isEmpty()) {
                log.error("No routes found in the database.");
                exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
                return exchange.getResponse().setComplete();
            }

            // Match route using prefix matching (consistent with other filters)
            GatewayRoute matchingRoute = null;
            for (GatewayRoute route : allRoutes) {
                String routePredicate = route.getPredicates();
                // Remove '/**' suffix if present for consistent matching
                if (routePredicate.endsWith("/**")) {
                    routePredicate = routePredicate.substring(0, routePredicate.length() - 3);
                }
                if (requestPath.startsWith(routePredicate)) {
                    matchingRoute = route;
                    break;
                }
            }

            if (matchingRoute == null) {
                log.warn("No matching route found for path={}", requestPath);
                return chain.filter(exchange);
            }

            log.info("Matching route found: {} with token validation enabled: {}",
                    matchingRoute.getRouteId(), matchingRoute.getWithToken());

            // Proceed with token validation only if enabled.
            if (!Boolean.TRUE.equals(matchingRoute.getWithToken())) {
                log.info("Token validation is disabled for route {}. Passing request along.", matchingRoute.getRouteId());
                return chain.filter(exchange);
            }

            HttpHeaders headers = exchange.getRequest().getHeaders();
            String authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Missing or invalid Authorization header for route {}.", matchingRoute.getRouteId());
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String token = authHeader.substring(7);
            log.info("Extracted token for route {}.", matchingRoute.getRouteId());
            final GatewayRoute finalRoute = matchingRoute;

            return Mono.just(token)
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap(jwtDecoder::decode)
                    .flatMap(jwt -> {
                        log.info("Token is valid for route {}.", finalRoute.getRouteId());
                        return chain.filter(exchange);
                    })
                    .onErrorResume(e -> {
                        log.warn("Token validation failed for route {}: {}", finalRoute.getRouteId(), e.getMessage());
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    });
        };
    }
}