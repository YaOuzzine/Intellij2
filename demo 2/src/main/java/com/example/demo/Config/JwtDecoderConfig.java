// demo 2/src/main/java/com/example/demo/Config/JwtDecoderConfig.java
package com.example.demo.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration
public class JwtDecoderConfig {

    @Value("${jwt.secret:mysupersecretkey}")
    private String jwtSecret;

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        // For a temporary solution, we'll use a simple symmetric key
        // This matches our simplified JwtUtil approach
        return NimbusReactiveJwtDecoder.withSecretKey(
                javax.crypto.spec.SecretKeySpec.class.cast(
                        new javax.crypto.spec.SecretKeySpec(
                                jwtSecret.getBytes(),
                                "HmacSHA256"
                        )
                )
        ).build();
    }
}