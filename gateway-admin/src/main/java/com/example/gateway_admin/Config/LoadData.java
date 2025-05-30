// gateway-admin/src/main/java/com/example/gateway_admin/Config/LoadData.java
package com.example.gateway_admin.Config;

import com.example.gateway_admin.Entities.AllowedIps;
import com.example.gateway_admin.Entities.GatewayRoute;
import com.example.gateway_admin.Entities.RateLimit;
import com.example.gateway_admin.Entities.User; // Import User entity
import com.example.gateway_admin.Repositories.AllowedIpRepository;
import com.example.gateway_admin.Repositories.GatewayRouteRepository;
import com.example.gateway_admin.Repositories.RateLimitRepository;
import com.example.gateway_admin.Repositories.UserRepository; // Import UserRepository
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder; // Import PasswordEncoder

import java.util.logging.Logger;

@Configuration
@Profile("init")
public class LoadData {

    private static final Logger logger = Logger.getLogger(LoadData.class.getName());

    @Bean
    public CommandLineRunner dataLoader(
            GatewayRouteRepository routeRepo,
            AllowedIpRepository ipRepo,
            RateLimitRepository rateLimitRepo,
            UserRepository userRepository,         // Inject UserRepository
            PasswordEncoder passwordEncoder       // Inject PasswordEncoder
    ) {
        return args -> {
            logger.info("Starting initial data loading - only runs with 'init' profile active");

            // --- Create Admin User if not exists ---
            if (userRepository.findByUsername("admin").isEmpty()) {
                User adminUser = new User();
                adminUser.setUsername("admin");
                adminUser.setPassword(passwordEncoder.encode("admin123")); // Encode password
                adminUser.setFirstName("Admin");
                adminUser.setLastName("User");
                adminUser.setEmail("admin@example.com");
                adminUser.setRole("ADMIN");
                adminUser.setActive(true);
                userRepository.save(adminUser);
                logger.info("Created admin user with username 'admin'");
            } else {
                logger.info("Admin user 'admin' already exists.");
            }

            // --- Route #1: IP filtering disabled, token validation and rate limiting disabled
            String predicate1 = "/server-final/**";
            if (routeRepo.findByPredicates(predicate1) == null) {
                GatewayRoute route1 = new GatewayRoute();
                route1.setRouteId("final-server1-secure-route");
                route1.setUri("http://localhost:8050");
                route1.setPredicates(predicate1);
                route1.setWithIpFilter(false);
                route1.setWithToken(false); // Example: public route
                route1.setWithRateLimit(false);
                route1 = routeRepo.save(route1);

                AllowedIps ip1 = new AllowedIps();
                ip1.setIp("192.168.10.101"); // Example IP, not used if withIpFilter is false
                ip1.setGatewayRoute(route1);
                ipRepo.save(ip1);

                RateLimit rl1 = new RateLimit();
                rl1.setRouteId(route1.getId());
                rl1.setMaxRequests(100);
                rl1.setTimeWindowMs(60000);
                rateLimitRepo.save(rl1);
                route1.setRateLimit(rl1);
                routeRepo.save(route1);
                logger.info("Route #1 created: " + route1.getPredicates());
            }

            // --- Route #2: IP filtering enabled, rate limiting enabled; token validation enabled
            String predicate2 = "/server-final2/**";
            if (routeRepo.findByPredicates(predicate2) == null) {
                GatewayRoute route2 = new GatewayRoute();
                route2.setRouteId("final-server2-secure-route");
                route2.setUri("http://localhost:8060");
                route2.setPredicates(predicate2);
                route2.setWithIpFilter(true);
                route2.setWithToken(true); // Example: secured route
                route2.setWithRateLimit(true);
                route2 = routeRepo.save(route2);

                AllowedIps ip2 = new AllowedIps();
                ip2.setIp("127.0.0.1");
                ip2.setGatewayRoute(route2);
                ipRepo.save(ip2);

                RateLimit rl2 = new RateLimit();
                rl2.setRouteId(route2.getId());
                rl2.setMaxRequests(10);
                rl2.setTimeWindowMs(60000);
                rateLimitRepo.save(rl2);
                route2.setRateLimit(rl2);
                routeRepo.save(route2);
                logger.info("Route #2 created: " + route2.getPredicates());
            }
            logger.info("Initial data loading process completed.");
        };
    }
}