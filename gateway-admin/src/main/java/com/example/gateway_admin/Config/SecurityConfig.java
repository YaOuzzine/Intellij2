// gateway-admin/src/main/java/com/example/gateway_admin/Config/SecurityConfig.java
package com.example.gateway_admin.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${jwt.secret:lB56pF9DgJuJcOuza8zT4MTxuhjLJI/BqrlbcFA87Mc=}")
    private String jwtSecret;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        logger.info("Creating JWT decoder with secret key");
        SecretKeySpec secretKey = new SecretKeySpec(jwtSecret.getBytes(), "HmacSHA256");
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withSecretKey(secretKey).build();

        // Add debug logging to the decoder
        return token -> {
            logger.debug("Attempting to decode JWT token: {}", token.substring(0, Math.min(token.length(), 20)) + "...");
            return decoder.decode(token)
                    .doOnSuccess(jwt -> {
                        logger.info("Successfully decoded JWT token");
                        logger.debug("JWT Claims: {}", jwt.getClaims());
                        logger.debug("JWT Subject: {}", jwt.getSubject());
                    })
                    .doOnError(error -> {
                        logger.error("Failed to decode JWT token: {}", error.getMessage(), error);
                    });
        };
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        logger.info("Configuring security web filter chain");

        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeExchange(exchanges -> {
                    logger.info("Configuring security authorization rules");
                    exchanges
                            .pathMatchers("/api/auth/**").permitAll()
                            // Temporarily allow all to diagnose authentication issues
                            .anyExchange().permitAll();
                    logger.info("Security rules configured");
                })
                .oauth2ResourceServer(oauth2 -> {
                    logger.info("Configuring OAuth2 resource server");
                    oauth2.jwt(jwt -> {
                        jwt.jwtDecoder(jwtDecoder());
                        logger.info("JWT authentication configured with decoder");
                    });
                })
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        logger.info("Configuring CORS");
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        logger.info("CORS configured successfully");
        return source;
    }
}