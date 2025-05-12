// gateway-admin/src/main/java/com/example/gateway_admin/Config/SecurityConfig.java
package com.example.gateway_admin.Config;

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
import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    // Use the same secret key as in the demo project
    @Value("${jwt.secret:lB56pF9DgJuJcOuza8zT4MTxuhjLJI/BqrlbcFA87Mc=}")
    private String jwtSecret;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        // Use symmetric key validation to match our JWT generation approach
        return NimbusReactiveJwtDecoder.withSecretKey(
                new SecretKeySpec(jwtSecret.getBytes(), "HmacSHA256")
        ).build();
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/api/auth/**").permitAll() // Should not be relevant for gateway-admin, but good practice
                        // Change here: accept any authenticated user for user profile
                        .pathMatchers("/api/user/profile").authenticated()
                        .pathMatchers("/api/gateway-routes/**").authenticated() // Changed from hasAuthority
                        .pathMatchers("/api/ip-addresses/**").authenticated() // Changed from hasAuthority
                        .pathMatchers("/api/rate-limit/**").authenticated() // Changed from hasAuthority
                        .pathMatchers("/api/user/**").authenticated() // Changed from hasAuthority
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