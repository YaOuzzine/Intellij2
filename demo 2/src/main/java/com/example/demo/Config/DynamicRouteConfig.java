// src/main/java/com/example/demo/Config/DynamicRouteConfig.java
package com.example.demo.Config;

import com.example.demo.Entity.AllowedIp;
import com.example.demo.Entity.GatewayRoute;
import com.example.demo.Repository.GatewayRouteRepository;
import com.example.demo.Filter.IpValidationGatewayFilterFactory;
import com.example.demo.Filter.TokenValidationGatewayFilterFactory;
import com.example.demo.Filter.SimpleRateLimitGatewayFilterFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
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
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
            List<GatewayRoute> dbRoutes = repo.findAll();
            // Longer predicate first  →  “/server-final2/**” before “/server-final/**”
            dbRoutes.sort(Comparator.comparingInt((GatewayRoute r) ->
                    r.getPredicates() == null ? 0 : r.getPredicates().length()).reversed());

            System.out.println("Route build order (longest path first):");
            dbRoutes.forEach(r -> System.out.println("  • " + r.getPredicates()));

            PathRoutePredicateFactory pathFactory = new PathRoutePredicateFactory();
            List<Route> routeDefs = new ArrayList<>();

            for (GatewayRoute r : dbRoutes) {

                /* ---- basic validation ---- */
                if (r.getPredicates() == null || r.getUri() == null) continue;

                String routeId = (r.getRouteId() == null || r.getRouteId().isBlank())
                        ? "route-" + r.getId() : r.getRouteId();

                PathRoutePredicateFactory.Config pc = new PathRoutePredicateFactory.Config();
                pc.setPatterns(Collections.singletonList(r.getPredicates()));
                Predicate<ServerWebExchange> pathPred = pathFactory.apply(pc);

                String raw = r.getUri().contains("://") ? r.getUri() : "http://" + r.getUri();

                Route.AsyncBuilder b = Route.async()
                        .id(routeId)
                        .uri(URI.create(raw))
                        .predicate(pathPred)
                        .metadata("withIpFilter", r.getWithIpFilter())
                        .metadata("withToken",    r.getWithToken())
                        .metadata("withRateLimit",r.getWithRateLimit());

                if (Boolean.TRUE.equals(r.getWithIpFilter())) {
                    List<String> ips = r.getAllowedIps() == null ? Collections.emptyList()
                            : r.getAllowedIps().stream().map(AllowedIp::getIp).collect(Collectors.toList());
                    b.metadata("allowedIps", ips)
                            .filter(ipFactory.apply((Void) null));
                }
                if (Boolean.TRUE.equals(r.getWithToken())) {
                    b.filter(tokenFactory.apply((Void) null));
                }
                if (Boolean.TRUE.equals(r.getWithRateLimit()) && r.getRateLimit() != null) {
                    b.metadata("maxRequests", r.getRateLimit().getMaxRequests())
                            .metadata("timeWindowMs", r.getRateLimit().getTimeWindowMs())
                            .filter(rlFactory.apply((Void) null));
                }
                routeDefs.add(b.build());
            }
            return Flux.fromIterable(routeDefs);

        }).subscribeOn(Schedulers.boundedElastic());
    }

    /** Trigger after any CRUD change to refresh gateway routes */
    public void publishRefreshEvent() {
        publisher.publishEvent(new RefreshRoutesEvent(this));
    }

    /**
     * Periodically refresh routes from the database
     */
    @Scheduled(fixedDelay = 45000) // 45 seconds (slightly longer than the admin sync)
    public void refreshRoutes() {
        this.publishRefreshEvent();
    }
}
