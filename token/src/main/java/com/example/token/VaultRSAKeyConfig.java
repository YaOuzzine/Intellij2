package com.example.token;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.vault.core.VaultTemplate;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.cloud.vault.enabled", havingValue = "true")
public class VaultRSAKeyConfig {

    private final VaultTemplate vaultTemplate;

    public VaultRSAKeyConfig(VaultTemplate vaultTemplate) {
        this.vaultTemplate = vaultTemplate;
    }

    @Bean
    @Primary
    public RSAPrivateKey rsaPrivateKey() {
        try {
            // Read the secret from Vault (KV v2 path)
            Map<String, Object> secretData = vaultTemplate.read("secret/data/myapp/rsa/private_key").getData();
            Map<String, Object> data = (Map<String, Object>) secretData.get("data");
            String privateKeyPem = (String) data.get("private_key");

            // Remove PEM header/footer by filtering out lines starting with "-----"
            String keyContent = Arrays.stream(privateKeyPem.split("\\r?\\n"))
                    .filter(line -> !line.startsWith("-----"))
                    .collect(Collectors.joining());

            byte[] decodedKey = Base64.getDecoder().decode(keyContent);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(decodedKey));
        } catch (Exception e) {
            log.error("Error loading private key from Vault: {}", e.getMessage(), e);
            throw new RuntimeException("Unable to load private key from Vault", e);
        }
    }

    @Bean
    @Primary
    public RSAPublicKey rsaPublicKey() {
        try {
            ClassPathResource resource = new ClassPathResource("keys/public_key.pem");
            byte[] keyBytes = Files.readAllBytes(Paths.get(resource.getURI()));
            String keyString = new String(keyBytes)
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(keyString);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(decoded));
        } catch (Exception e) {
            throw new RuntimeException("Unable to load public key", e);
        }
    }
}
