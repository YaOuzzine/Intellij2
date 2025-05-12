// demo 2/src/main/java/com/example/demo/Config/JwtUtil.java
package com.example.demo.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64; // Ensure this import is present
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Simple JWT utility that doesn't rely on the JJWT library
 * This is a temporary fix until the JJWT dependency issues are resolved
 */
@Component
public class JwtUtil {

    // Use a simple secret key for development
    @Value("${jwt.secret:mysupersecretkey}")
    private String secret;

    @Value("${jwt.expiration-ms:3600000}")
    private long jwtExpirationMs;

    @Value("${jwt.issuer:my-app}")
    private String jwtIssuer;

    /**
     * Generate a simple JWT token without relying on the JJWT library
     */
    public String generateToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        List<String> roles = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        // Create JWT header
        Map<String, Object> header = new HashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");
        String encodedHeader = base64UrlEncode(toJson(header));

        // Create JWT payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("sub", userPrincipal.getUsername());
        payload.put("roles", roles);
        payload.put("iss", jwtIssuer);
        payload.put("iat", now.getTime() / 1000);
        payload.put("exp", expiryDate.getTime() / 1000);
        payload.put("jti", UUID.randomUUID().toString());
        String encodedPayload = base64UrlEncode(toJson(payload));

        // Create signature
        String signature = calculateHmacSha256(encodedHeader + "." + encodedPayload, secret);

        // Combine all parts to form the JWT
        return encodedHeader + "." + encodedPayload + "." + signature;
    }

    private String toJson(Map<String, Object> map) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                json.append(",");
            }
            first = false;
            json.append("\"").append(entry.getKey()).append("\":");

            if (entry.getValue() instanceof String) {
                json.append("\"").append(entry.getValue()).append("\"");
            } else if (entry.getValue() instanceof Number) {
                json.append(entry.getValue());
            } else if (entry.getValue() instanceof List) {
                List<?> list = (List<?>) entry.getValue();
                json.append("[");
                boolean firstItem = true;
                for (Object item : list) {
                    if (!firstItem) json.append(",");
                    firstItem = false;
                    if (item instanceof String) {
                        json.append("\"").append(item).append("\"");
                    } else {
                        json.append(item);
                    }
                }
                json.append("]");
            } else {
                json.append(entry.getValue());
            }
        }
        json.append("}");
        return json.toString();
    }

    private String calculateHmacSha256(String data, String key) {
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            // CRITICAL: Use consistent encoding for the key
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);
            // CRITICAL: Use consistent encoding for the data to be signed
            byte[] signatureBytes = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            // ***** THIS IS THE CORRECTED LINE *****
            // Directly Base64URL encode the raw signature bytes
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);
            // **************************************

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Error calculating HMAC: " + e.getMessage(), e);
        }
    }

    // This method is for encoding STRING inputs (like JSON header/payload)
    // and should remain as it was.
    private String base64UrlEncode(String input) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }
}