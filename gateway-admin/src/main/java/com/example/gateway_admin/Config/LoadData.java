package com.example.gateway_admin.Config;

import com.example.gateway_admin.Entities.AllowedIps;
import com.example.gateway_admin.Entities.GatewayRoute;
import com.example.gateway_admin.Entities.RateLimit;
import com.example.gateway_admin.Repositories.AllowedIpRepository;
import com.example.gateway_admin.Repositories.GatewayRouteRepository;
import com.example.gateway_admin.Repositories.RateLimitRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Optional;
import java.util.logging.Logger;

@Configuration
@Profile("init") // Add a profile so this only runs when specifically enabled
public class LoadData {

    private static final Logger logger = Logger.getLogger(LoadData.class.getName());

    @Bean
    public CommandLineRunner dataLoader(
            GatewayRouteRepository routeRepo,
            AllowedIpRepository ipRepo,
            RateLimitRepository rateLimitRepo
    ) {
        return args -> {
            logger.info("Starting initial data loading - only runs with 'init' profile active");

            // Check if we already have data
            long routeCount = routeRepo.count();
            if (routeCount > 0) {
                logger.info("Data already exists! Found " + routeCount + " routes. Skipping initial data loading.");
                return; // Skip initialization if we already have data
            }

            logger.info("No existing routes found. Creating initial sample data...");

            // --- Route #1: IP filtering disabled, token validation and rate limiting disabled
            String predicate1 = "/server-final/**";
            GatewayRoute route1 = routeRepo.findByPredicates(predicate1);
            if (route1 == null) {
                route1 = new GatewayRoute();
                route1.setRouteId("final-server1-secure-route");
                route1.setUri("http://localhost:8050");
                route1.setPredicates(predicate1);
                route1.setWithIpFilter(false);
                route1.setWithToken(false);
                route1.setWithRateLimit(false);
                route1 = routeRepo.save(route1);

                // Only create IP if this is a new route
                AllowedIps ip1 = new AllowedIps();
                ip1.setIp("192.168.10.101");
                ip1.setGatewayRoute(route1);
                ipRepo.save(ip1);

                // Only create rate limit if this is a new route
                RateLimit rl1 = new RateLimit();
                rl1.setRouteId(route1.getId());
                rl1.setMaxRequests(100);
                rl1.setTimeWindowMs(60000);
                rateLimitRepo.save(rl1);
                route1.setRateLimit(rl1);
                routeRepo.save(route1);

                logger.info("Route #1 created: " + route1.getPredicates());
            }

            // --- Route #2: IP filtering enabled, rate limiting enabled; token validation disabled
            String predicate2 = "/server-final2/**";
            GatewayRoute route2 = routeRepo.findByPredicates(predicate2);
            if (route2 == null) {
                route2 = new GatewayRoute();
                route2.setRouteId("final-server2-secure-route");
                route2.setUri("http://localhost:8060");
                route2.setPredicates(predicate2);
                route2.setWithIpFilter(true);
                route2.setWithToken(false);
                route2.setWithRateLimit(true);
                route2 = routeRepo.save(route2);

                // Only create IP if this is a new route
                AllowedIps ip2 = new AllowedIps();
                ip2.setIp("127.0.0.1");
                ip2.setGatewayRoute(route2);
                ipRepo.save(ip2);

                // Only create rate limit if this is a new route
                RateLimit rl2 = new RateLimit();
                rl2.setRouteId(route2.getId());
                rl2.setMaxRequests(10);
                rl2.setTimeWindowMs(60000);
                rateLimitRepo.save(rl2);
                route2.setRateLimit(rl2);
                routeRepo.save(route2);

                logger.info("Route #2 created: " + route2.getPredicates());
            }

            logger.info("Initial data loading completed successfully!");
        };
    }
}