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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
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
        logger.info("Creating JWT decoder with secret key: '{}' (first 10 chars)",
                jwtSecret.substring(0, Math.min(10, jwtSecret.length())));

        // CRITICAL: Ensure we use the same charset encoding as the JWT generator
        byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        logger.debug("Secret key bytes length: {}", secretBytes.length);

        // Create the secret key with the correct algorithm (must match the one used in JwtUtil.java)
        SecretKeySpec secretKey = new SecretKeySpec(secretBytes, "HmacSHA256");

        // Create the JWT decoder with the secret key
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withSecretKey(secretKey).build();

        // Wrap the decoder with extensive logging
        return token -> {
            logger.debug("=============== JWT VALIDATION ATTEMPT ===============");
            logger.debug("Attempting to decode JWT token: {}", token);

            // Log individual parts of the JWT for debugging
            String[] parts = token.split("\\.");
            if (parts.length == 3) {
                logger.debug("Header (encoded): {}", parts[0]);
                logger.debug("Payload (encoded): {}", parts[1]);
                logger.debug("Signature (encoded): {}", parts[2]);

                // Decode and log the header and payload
                try {
                    String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
                    String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                    logger.debug("Header (decoded): {}", headerJson);
                    logger.debug("Payload (decoded): {}", payloadJson);

                    // Verify the signature manually to help debug
                    String dataToSign = parts[0] + "." + parts[1];
                    javax.crypto.Mac sha256Hmac = javax.crypto.Mac.getInstance("HmacSHA256");
                    sha256Hmac.init(secretKey);
                    byte[] signatureBytes = sha256Hmac.doFinal(dataToSign.getBytes(StandardCharsets.UTF_8));
                    String calculatedSignature = Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);

                    logger.debug("Expected signature: {}", calculatedSignature);
                    logger.debug("Received signature: {}", parts[2]);
                    logger.debug("Signatures match: {}", calculatedSignature.equals(parts[2]));
                } catch (Exception e) {
                    logger.error("Error manually verifying JWT: {}", e.getMessage(), e);
                }
            } else {
                logger.error("Invalid JWT format - expected 3 parts but got {}", parts.length);
            }

            return decoder.decode(token)
                    .doOnSuccess(jwt -> {
                        logger.info("Successfully decoded JWT token for subject: {}", jwt.getSubject());
                        logger.debug("JWT Claims: {}", jwt.getClaims());
                    })
                    .doOnError(error -> {
                        logger.error("Failed to decode JWT token: {} ({})",
                                error.getMessage(), error.getClass().getName());
                        if (error.getCause() != null) {
                            logger.error("Caused by: {} ({})",
                                    error.getCause().getMessage(),
                                    error.getCause().getClass().getName());
                        }
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
                            .anyExchange().authenticated();
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