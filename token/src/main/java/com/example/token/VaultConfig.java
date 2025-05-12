package com.example.token;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.vault.config.VaultProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;

@Configuration
@ConfigurationProperties("spring.cloud.vault")
@Primary  // Marking this configuration as primary so that its VaultProperties bean is preferred
public class VaultConfig extends VaultProperties {

    @Bean
    @ConditionalOnProperty(name = "spring.cloud.vault.enabled", havingValue = "true")
    public VaultEndpoint vaultEndpoint() {
        VaultEndpoint vaultEndpoint = VaultEndpoint.create(getHost(), getPort());
        vaultEndpoint.setScheme(getScheme());
        return vaultEndpoint;
    }

    @Bean
    @ConditionalOnProperty(name = "spring.cloud.vault.enabled", havingValue = "true")
    public VaultTemplate vaultTemplate() {
        return new VaultTemplate(vaultEndpoint(), clientAuthentication());
    }

    @Bean
    @ConditionalOnProperty(name = "spring.cloud.vault.enabled", havingValue = "true")
    public ClientAuthentication clientAuthentication() {
        return new TokenAuthentication(getToken());
    }
}
