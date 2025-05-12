/*
package com.example.token;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestConfiguration
public class TestConfig {

    @Bean
    public VaultTemplate vaultTemplate() {
        VaultTemplate mockVaultTemplate = mock(VaultTemplate.class);
        VaultResponse mockResponse = mock(VaultResponse.class);

        // Create mock data structure
        Map<String, Object> mockData = new HashMap<>();
        Map<String, String> keyData = new HashMap<>();
        keyData.put("private_key", "-----BEGIN PRIVATE KEY-----\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDHxA7V5WQBJ8IJ\n-----END PRIVATE KEY-----");
        keyData.put("public_key", "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAx8QO1eVkASfCCQ==\n-----END PUBLIC KEY-----");
        mockData.put("data", keyData);

        when(mockResponse.getData()).thenReturn(mockData);
        when(mockVaultTemplate.read(anyString())).thenReturn(mockResponse);

        return mockVaultTemplate;
    }
}*/
