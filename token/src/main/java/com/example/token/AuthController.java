package com.example.token;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtUtil jwtUtil;
    private final RSAPublicKey rsaPublicKey;

    public AuthController(JwtUtil jwtUtil, RSAPublicKey rsaPublicKey) {
        this.jwtUtil = jwtUtil;
        this.rsaPublicKey = rsaPublicKey;
    }

    @PostMapping("/token")
    public ResponseEntity<String> generateToken(@RequestHeader("X-Client-Id") String clientId) {
        if (clientId != null && !clientId.isEmpty()) {
            String token = jwtUtil.generateToken(clientId);
            return ResponseEntity.ok(token);
        }
        return ResponseEntity.status(401).body("Invalid client");
    }

    @GetMapping("/public-key")
    public ResponseEntity<String> getPublicKey() {
        String publicKeyPEM = "-----BEGIN PUBLIC KEY-----\n" +
                Base64.getEncoder().encodeToString(rsaPublicKey.getEncoded()) +
                "\n-----END PUBLIC KEY-----";
        return ResponseEntity.ok(publicKeyPEM);
    }
}

