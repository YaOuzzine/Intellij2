// gateway-admin/src/main/java/com/example/gateway_admin/Config/SecurityConfig.java
package com.example.gateway_admin.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource; // Ensure this import is present
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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    // This annotation tells Spring to load the resource from the classpath.
    // "classpath:keys/public_key.pem" resolves to "src/main/resources/keys/public_key.pem"
    @Value("classpath:keys/public_key.pem")
    private Resource publicKeyResource;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        try (InputStream inputStream = publicKeyResource.getInputStream()) {
            String pem = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            String cleanedPem = pem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", ""); // Remove all whitespace, including newlines
            byte[] decodedKey = Base64.getDecoder().decode(cleanedPem);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(keySpec);
            return NimbusReactiveJwtDecoder.withPublicKey(publicKey).build();
        } catch (Exception e) {
            // Log the error for better debugging
            System.err.println("Error loading public key from resource: " + publicKeyResource);
            e.printStackTrace();
            throw new RuntimeException("Failed to load public key for JWT decoder", e);
        }
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/api/auth/**").permitAll() // Should not be relevant for gateway-admin, but good practice
                        .pathMatchers("/api/gateway-routes/**").hasAuthority("SCOPE_ADMIN")
                        .pathMatchers("/api/ip-addresses/**").hasAuthority("SCOPE_ADMIN")
                        .pathMatchers("/api/rate-limit/**").hasAuthority("SCOPE_ADMIN")
                        .pathMatchers("/api/user/profile").authenticated()
                        .pathMatchers("/api/user/**").hasAuthority("SCOPE_ADMIN")
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtDecoder(jwtDecoder())));
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}