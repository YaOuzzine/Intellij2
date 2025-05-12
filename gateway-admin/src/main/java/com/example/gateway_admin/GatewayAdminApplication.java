package com.example.gateway_admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GatewayAdminApplication {
	public static void main(String[] args) {
		SpringApplication.run(GatewayAdminApplication.class, args);
	}
}