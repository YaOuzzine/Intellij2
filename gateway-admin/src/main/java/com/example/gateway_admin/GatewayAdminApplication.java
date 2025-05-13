// gateway-admin/src/main/java/com/example/gateway_admin/GatewayAdminApplication.java

package com.example.gateway_admin;

import com.example.gateway_admin.Services.StorageService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GatewayAdminApplication {
	public static void main(String[] args) {
		SpringApplication.run(GatewayAdminApplication.class, args);
	}

	@Bean
	CommandLineRunner init(StorageService storageService) {
		return (args) -> {
			storageService.init();
		};
	}
}