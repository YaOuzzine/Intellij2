package com.example.demo.Config;

import com.example.demo.Entity.AllowedIp;
import com.example.demo.Entity.GatewayRoute;
import com.example.demo.Repository.GatewayRouteRepository;
import com.example.demo.Filter.IpValidationGatewayFilterFactory;
import com.example.demo.Filter.TokenValidationGatewayFilterFactory;
import com.example.demo.Filter.SimpleRateLimitGatewayFilterFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.RewritePathGatewayFilterFactory;
import org.springframework.cloud.gateway.handler.predicate.PathRoutePredicateFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Configuration
public class DynamicRouteConfig {

    @Autowired
    private ApplicationEventPublisher publisher;

    @Bean
    public RouteLocator customRouteLocator(
            GatewayRouteRepository repo,
            IpValidationGatewayFilterFactory ipFactory,
            TokenValidationGatewayFilterFactory tokenFactory,
            SimpleRateLimitGatewayFilterFactory rlFactory
    ) {
        return () -> Flux.defer(() -> {

            /* ---------- pull & sort routes --------------- */
            // Use the method that eagerly loads relationships
            List<GatewayRoute> dbRoutes = repo.findAllWithAllowedIpsAndRateLimit();

            if (dbRoutes.isEmpty()) {
                log.error("No routes found in the database! The gateway will not function properly.");
                return Flux.empty();
            }

            // Longer predicate first → "/server-final2/**" before "/server-final/**"
            dbRoutes.sort(Comparator.comparingInt((GatewayRoute r) ->
                    r.getPredicates() == null ? 0 : r.getPredicates().length()).reversed());

            log.info("Route build order (longest path first):");
            dbRoutes.forEach(r -> log.info("  • {} -> {} (IP: {}, Token: {}, RateLimit: {})",
                    r.getPredicates(),
                    r.getUri(),
                    r.getWithIpFilter(),
                    r.getWithToken(),
                    r.getWithRateLimit()));

            // Print rate limit info if available
            dbRoutes.stream()
                    .filter(r -> Boolean.TRUE.equals(r.getWithRateLimit()) && r.getRateLimit() != null)
                    .forEach(r -> log.info("    Rate limit for {}: {} requests per {} ms",
                            r.getPredicates(),
                            r.getRateLimit().getMaxRequests(),
                            r.getRateLimit().getTimeWindowMs()));

            PathRoutePredicateFactory pathFactory = new PathRoutePredicateFactory();
            List<Route> routeDefs = new ArrayList<>();

            for (GatewayRoute r : dbRoutes) {

                /* ---- basic validation ---- */
                if (r.getPredicates() == null || r.getUri() == null) {
                    log.warn("Skipping route with null predicate or URI: {}", r.getId());
                    continue;
                }

                String routeId = (r.getRouteId() == null || r.getRouteId().isBlank())
                        ? "route-" + r.getId() : r.getRouteId();

                PathRoutePredicateFactory.Config pc = new PathRoutePredicateFactory.Config();
                pc.setPatterns(Collections.singletonList(r.getPredicates()));
                Predicate<ServerWebExchange> pathPred = pathFactory.apply(pc);

                String raw = r.getUri().contains("://") ? r.getUri() : "http://" + r.getUri();

                // Test URI validity
                URI uri;
                try {
                    uri = new URI(raw);
                    log.info("Created route: {} with URI: {} and pattern: {}", routeId, uri, r.getPredicates());
                } catch (URISyntaxException e) {
                    log.error("Invalid URI format for route {}: {}. Skipping this route.", routeId, raw, e);
                    continue;
                }

                // Create route builder with predicate and metadata
                Route.AsyncBuilder routeBuilder = Route.async()
                        .id(routeId)
                        .uri(uri)
                        .predicate(pathPred)
                        .metadata("withIpFilter", r.getWithIpFilter())
                        .metadata("withToken", r.getWithToken())
                        .metadata("withRateLimit", r.getWithRateLimit());

                // Add appropriate filters based on configuration
                List<GatewayFilter> filters = new ArrayList<>();

                if (Boolean.TRUE.equals(r.getWithIpFilter())) {
                    List<String> ips = r.getAllowedIps() == null ? Collections.emptyList()
                            : r.getAllowedIps().stream().map(AllowedIp::getIp).collect(Collectors.toList());
                    routeBuilder.metadata("allowedIps", ips);
                    filters.add(ipFactory.apply((Void) null));
                    log.info("Added IP filter to route {}", routeId);
                }

                // IMPORTANT CHANGE: Always add the token filter
                // The filter itself will check the withToken flag and skip validation if needed
                filters.add(tokenFactory.apply((Void) null));
                log.info("Added token filter to route {} (withToken = {})", routeId, r.getWithToken());

                if (Boolean.TRUE.equals(r.getWithRateLimit()) && r.getRateLimit() != null) {
                    routeBuilder.metadata("maxRequests", r.getRateLimit().getMaxRequests())
                            .metadata("timeWindowMs", r.getRateLimit().getTimeWindowMs());
                    filters.add(rlFactory.apply((Void) null));
                    log.info("Added rate limit filter to route {} with {} requests per {} ms",
                            routeId, r.getRateLimit().getMaxRequests(), r.getRateLimit().getTimeWindowMs());
                }

                // Add all filters to the route
                for (GatewayFilter filter : filters) {
                    routeBuilder.filter(filter);
                }

                // Add the route to our list
                routeDefs.add(routeBuilder.build());
                log.info("Route {} has been successfully built with {} filters", routeId, filters.size());
            }

            log.info("Total routes created: {}", routeDefs.size());
            if (routeDefs.isEmpty()) {
                log.warn("No valid routes were created! The gateway will not function properly.");
            }

            return Flux.fromIterable(routeDefs);

        }).subscribeOn(Schedulers.boundedElastic());
    }

    /** Trigger after any CRUD change to refresh gateway routes */
    public void publishRefreshEvent() {
        log.info("Publishing RefreshRoutesEvent to update gateway routes");
        publisher.publishEvent(new RefreshRoutesEvent(this));
    }

    /**
     * Periodically refresh routes from the database
     */
    @Scheduled(fixedDelay = 45000) // 45 seconds (slightly longer than the admin sync)
    public void refreshRoutes() {
        log.info("Refreshing routes automatically...");
        this.publishRefreshEvent();
    }
}
