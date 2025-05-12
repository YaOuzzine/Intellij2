/*
package com.example.demo.Db;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Service
public class PublicKeyService {

    private final ResourceLoader resourceLoader;

    @Autowired
    public PublicKeyService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    */
/**
     * Reads the public key from a file located at: resources/keys/public_key.pem
     * and returns it as an RSAPublicKey.
     *//*

    public RSAPublicKey getPublicKey() {
        try {
            // 1) Load the file from the classpath
            Resource resource = resourceLoader.getResource("classpath:keys/public_key.pem");

            // 2) Read the file's bytes and convert to String
            byte[] keyBytes = resource.getInputStream().readAllBytes();
            String publicKeyContent = new String(keyBytes, StandardCharsets.UTF_8);

            // 3) Strip the PEM header/footer and whitespace
            publicKeyContent = publicKeyContent
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");

            // 4) Decode the Base64 text into a byte array
            byte[] decodedKey = Base64.getDecoder().decode(publicKeyContent);

            // 5) Use KeyFactory to convert to RSAPublicKey
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedKey);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) kf.generatePublic(keySpec);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load public key from file", e);
        }
    }
}
*/
