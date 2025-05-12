package com.example.demo.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration
public class JwtDecoderConfig {

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        try {
            // Specify the absolute file path to the PEM file in the token module.
            String filePath = "C:\\Users\\yassi\\Documents\\PFE\\PFE-Intellij\\token\\src\\main\\resources\\Keys\\public_key.pem";
            try (FileInputStream fis = new FileInputStream(filePath)) {
                // Read the file content as a String.
                String pem = new String(fis.readAllBytes(), StandardCharsets.UTF_8);

                // Remove PEM header, footer, and whitespace/newlines.
                String cleanedPem = pem
                        .replace("-----BEGIN PUBLIC KEY-----", "")
                        .replace("-----END PUBLIC KEY-----", "")
                        .replaceAll("\\s+", "");

                // Decode the Base64 content.
                byte[] decoded = Base64.getDecoder().decode(cleanedPem);

                // Create an X509EncodedKeySpec from the decoded bytes.
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);

                // Generate an RSA public key.
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(keySpec);

                // Build and return the ReactiveJwtDecoder.
                return NimbusReactiveJwtDecoder.withPublicKey(publicKey).build();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load public key from file: " + e.getMessage(), e);
        }
    }
}