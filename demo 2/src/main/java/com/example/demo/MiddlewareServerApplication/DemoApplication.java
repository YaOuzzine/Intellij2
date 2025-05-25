package com.example.demo.MiddlewareServerApplication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.example.demo")
@EntityScan("com.example.demo.Entity")
@EnableJpaRepositories("com.example.demo.Repository")
@EnableScheduling
public class DemoApplication {
	public static void main(String[] args) {
		// Debug environment loading
		String openaiKey = System.getenv("OPENAI_API_KEY");
		System.out.println("=== Environment Debug ===");
		System.out.println("OPENAI_API_KEY from env: " + (openaiKey != null ? "PRESENT (length: " + openaiKey.length() + ")" : "NOT FOUND"));
		System.out.println("System property openai.api.key: " + System.getProperty("openai.api.key", "NOT SET"));

		if (openaiKey != null) {
			System.setProperty("openai.api.key", openaiKey);
			System.out.println("Set system property from environment variable");
		} else {
			System.out.println("WARNING: OPENAI_API_KEY environment variable not found");
		}

		SpringApplication.run(DemoApplication.class, args);
	}
}