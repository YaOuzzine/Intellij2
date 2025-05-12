package com.example.token;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;
import java.security.interfaces.RSAPrivateKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    private final RSAPrivateKey privateKey;

    public JwtUtil(RSAPrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    public String generateToken(String clientId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("clientId", clientId);
       // claims.put("roles", "USER");

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }
}
