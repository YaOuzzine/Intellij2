// demo 2/src/main/java/com/example/demo/Config/AnalyticsConfiguration.java
package com.example.demo.Config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableScheduling
public class AnalyticsConfiguration {

    /**
     * Task executor for async analytics operations
     */
    @Bean(name = "analyticsTaskExecutor")
    @ConditionalOnMissingBean(name = "analyticsTaskExecutor")
    public Executor analyticsTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("Analytics-");
        executor.initialize();
        return executor;
    }

    /**
     * Task executor for AI security analysis
     */
    @Bean(name = "aiSecurityTaskExecutor")
    @ConditionalOnMissingBean(name = "aiSecurityTaskExecutor")
    public Executor aiSecurityTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("AISecurity-");
        executor.initialize();
        return executor;
    }
}