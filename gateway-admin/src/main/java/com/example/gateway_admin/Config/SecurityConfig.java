// gateway-admin/src/main/java/com/example/gateway_admin/Config/SecurityConfig.java
package com.example.gateway_admin.Config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// Remove: import org.springframework.beans.factory.annotation.Value; // Not needed for dummy decoder
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt; // Import Jwt
import org.springframework.security.oauth2.jwt.JwtException; // Import JwtException
// Remove: import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder; // Not used
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono; // Import Mono

// Remove: import javax.crypto.spec.SecretKeySpec; // Not used
import java.nio.charset.StandardCharsets;
import java.time.Instant; // Import Instant
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections; // Import Collections
import java.util.List;
import java.util.Map; // Import Map

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
    private final ObjectMapper objectMapper = new ObjectMapper(); // For dummy decoder

    // @Value("${jwt.secret:lB56pF9DgJuJcOuza8zT4MTxuhjLJI/BqrlbcFA87Mc=}")
    // private String jwtSecret; // Not needed for the dummy decoder

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        logger.warn("Initializing MOCK ReactiveJwtDecoder for 'gateway-admin'. THIS BYPASSES JWT SIGNATURE VALIDATION.");
        return token -> Mono.fromCallable(() -> {
            logger.debug("MOCK JWT Decoder (gateway-admin) processing token: {}", token);
            try {
                String[] parts = token.split("\\.");
                if (parts.length < 2) { // A JWT typically has 3 parts, but payload is in the second.
                    logger.warn("Invalid JWT format for MOCK decoding (not enough parts) in gateway-admin: {}", token);
                    throw new JwtException("Invalid JWT format for mock decoding (gateway-admin)");
                }

                // Log individual parts for debugging if format is somewhat okay
                if (parts.length >= 2) {
                    logger.debug("Header (encoded) for MOCK (gateway-admin): {}", parts[0]);
                    logger.debug("Payload (encoded) for MOCK (gateway-admin): {}", parts[1]);
                    if (parts.length >=3) logger.debug("Signature (encoded) for MOCK (gateway-admin): {}", parts[2]);

                    try {
                        String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
                        String payloadJsonInternal = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                        logger.debug("Header (decoded for MOCK) (gateway-admin): {}", headerJson);
                        logger.debug("Payload (decoded for MOCK) (gateway-admin): {}", payloadJsonInternal);
                    } catch (Exception e) {
                        logger.warn("Could not Base64Url decode header/payload for logging during MOCK processing (gateway-admin)", e);
                    }
                }


                String payloadJson;
                try {
                    byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
                    payloadJson = new String(payloadBytes, StandardCharsets.UTF_8);
                } catch (IllegalArgumentException e) {
                    logger.error("Failed to Base64Url decode JWT payload for MOCK processing (gateway-admin): {}", e.getMessage());
                    throw new JwtException("Invalid JWT payload encoding for mock decoding (gateway-admin)", e);
                }

                Map<String, Object> claims = objectMapper.readValue(payloadJson, new TypeReference<Map<String, Object>>() {});
                logger.debug("MOCK decoded claims (gateway-admin): {}", claims);

                Instant issuedAt = claims.containsKey("iat") && claims.get("iat") instanceof Number ?
                        Instant.ofEpochSecond(((Number) claims.get("iat")).longValue()) :
                        Instant.now().minusSeconds(60);

                Instant expiresAt = claims.containsKey("exp") && claims.get("exp") instanceof Number ?
                        Instant.ofEpochSecond(((Number) claims.get("exp")).longValue()) :
                        Instant.now().plusSeconds(3600);

                // For "mimicking" and to be very lenient, let's ensure expiresAt is in the future.
                if (expiresAt.isBefore(Instant.now())) {
                    logger.warn("Original token was expired, MOCK decoder setting expiration to 1 hour from now for gateway-admin. Original exp: {}", expiresAt);
                    expiresAt = Instant.now().plusSeconds(3600);
                }

                Map<String, Object> headers = Collections.singletonMap("alg", "none");

                Jwt jwt = Jwt.withTokenValue(token)
                        .headers(h -> h.putAll(headers))
                        .claims(c -> c.putAll(claims))
                        .issuedAt(issuedAt)
                        .expiresAt(expiresAt)
                        .build();
                logger.info("Successfully MOCK decoded JWT token for subject (gateway-admin): {}", jwt.getSubject());
                return jwt;
            } catch (Exception e) {
                logger.error("Error in MOCK JWT decoding (gateway-admin): " + e.getMessage(), e);
                throw new JwtException("Failed to MOCK decode JWT (gateway-admin): " + e.getMessage(), e);
            }
        }).onErrorResume(JwtException.class, e -> {
            logger.error("MOCK JWT Decoder (gateway-admin) failed permanently for token: {}", token, e);
            return Mono.error(e);
        });
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        logger.info("Configuring security web filter chain for gateway-admin");

        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeExchange(exchanges -> {
                    logger.info("Configuring security authorization rules for gateway-admin");
                    exchanges
                            // Requests to /api/auth/** are typically for the auth server (demo2),
                            // but if gateway-admin has its own auth-related debug/status endpoints, permit them.
                            // Since login is proxied to demo2, this might be okay.
                            .pathMatchers("/api/auth/**").permitAll()
                            .pathMatchers("/actuator/**").permitAll() // Allow actuator endpoints
                            .anyExchange().authenticated();
                    logger.info("Security rules configured for gateway-admin");
                })
                .oauth2ResourceServer(oauth2 -> {
                    logger.info("Configuring OAuth2 resource server for gateway-admin");
                    oauth2.jwt(jwt -> {
                        jwt.jwtDecoder(jwtDecoder()); // Use the MOCK decoder
                        logger.info("JWT authentication configured with MOCK decoder for gateway-admin");
                    });
                })
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        logger.info("Configuring CORS for gateway-admin");
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173")); // Frontend URL
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        logger.info("CORS configured successfully for gateway-admin");
        return source;
    }
}