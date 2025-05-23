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
		// Load environment variables
		String openaiKey = System.getenv("OPENAI_API_KEY");
		if (openaiKey != null) {
			System.setProperty("openai.api.key", openaiKey);
		}

		SpringApplication.run(DemoApplication.class, args);
	}
}