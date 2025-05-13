// demo 2/src/main/java/com/example/demo/Config/JwtDecoderConfig.java
package com.example.demo.Config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

@Configuration
public class JwtDecoderConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtDecoderConfig.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        log.warn("Initializing MOCK ReactiveJwtDecoder for 'demo 2' (Gateway). THIS BYPASSES JWT SIGNATURE VALIDATION.");
        return token -> Mono.fromCallable(() -> {
            log.debug("MOCK JWT Decoder (demo 2) processing token: {}", token);
            try {
                String[] parts = token.split("\\.");
                if (parts.length < 2) { // A JWT typically has 3 parts, but payload is in the second.
                    log.warn("Invalid JWT format for MOCK decoding (not enough parts): {}", token);
                    throw new JwtException("Invalid JWT format for mock decoding");
                }

                String payloadJson;
                try {
                    byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
                    payloadJson = new String(payloadBytes, StandardCharsets.UTF_8);
                } catch (IllegalArgumentException e) {
                    log.error("Failed to Base64Url decode JWT payload for MOCK processing: {}", e.getMessage());
                    throw new JwtException("Invalid JWT payload encoding for mock decoding", e);
                }

                Map<String, Object> claims = objectMapper.readValue(payloadJson, new TypeReference<Map<String, Object>>() {});
                log.debug("MOCK decoded claims (demo 2): {}", claims);

                Instant issuedAt = claims.containsKey("iat") && claims.get("iat") instanceof Number ?
                        Instant.ofEpochSecond(((Number) claims.get("iat")).longValue()) :
                        Instant.now().minusSeconds(60); // Default to 1 minute ago if not present

                Instant expiresAt = claims.containsKey("exp") && claims.get("exp") instanceof Number ?
                        Instant.ofEpochSecond(((Number) claims.get("exp")).longValue()) :
                        Instant.now().plusSeconds(3600); // Default to 1 hour from now if not present

                // For "mimicking" and to be very lenient, let's ensure expiresAt is in the future.
                // This ensures Spring Security's default checks won't fail on expiration.
                if (expiresAt.isBefore(Instant.now())) {
                    log.warn("Original token was expired, MOCK decoder setting expiration to 1 hour from now for demo 2. Original exp: {}", expiresAt);
                    expiresAt = Instant.now().plusSeconds(3600);
                }


                Map<String, Object> headers = Collections.singletonMap("alg", "none"); // Indicate no signature was verified

                return Jwt.withTokenValue(token)
                        .headers(h -> h.putAll(headers))
                        .claims(c -> c.putAll(claims))
                        .issuedAt(issuedAt)
                        .expiresAt(expiresAt) // Use the potentially adjusted expiresAt
                        .build();
            } catch (Exception e) {
                log.error("Error in MOCK JWT decoding (demo 2): " + e.getMessage(), e);
                throw new JwtException("Failed to MOCK decode JWT (demo 2): " + e.getMessage(), e);
            }
        }).onErrorResume(JwtException.class, e -> {
            log.error("MOCK JWT Decoder (demo 2) failed permanently for token: {}", token, e);
            return Mono.error(e);
        });
    }
}