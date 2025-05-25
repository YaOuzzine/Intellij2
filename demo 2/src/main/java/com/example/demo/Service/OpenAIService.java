// demo 2/src/main/java/com/example/demo/Service/OpenAIService.java
package com.example.demo.Service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class OpenAIService {

    private static final Logger log = LoggerFactory.getLogger(OpenAIService.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${openai.api.key:}")
    private String apiKey;

    @Value("${openai.model:gpt-3.5-turbo}")
    private String model;

    @Value("${openai.max-tokens:2000}")
    private Integer maxTokens;

    @Value("${openai.temperature:0.2}")
    private Double temperature;

    public OpenAIService() {
        this.objectMapper = new ObjectMapper();
        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Mono<String> generateChatCompletion(String systemPrompt, String userPrompt) {
        log.info("=== OpenAI API Call Debug ===");
        log.info("API Key present: {}", apiKey != null && !apiKey.trim().isEmpty());
        log.info("API Key length: {}", apiKey != null ? apiKey.length() : 0);
        log.info("API Key starts with 'sk-': {}", apiKey != null && apiKey.startsWith("sk-"));
        log.info("Model: {}", model);
        log.info("Max Tokens: {}", maxTokens);
        log.info("Temperature: {}", temperature);
        log.info("System prompt length: {}", systemPrompt != null ? systemPrompt.length() : 0);
        log.info("User prompt length: {}", userPrompt != null ? userPrompt.length() : 0);

        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("OpenAI API key not configured, returning fallback response");
            return Mono.just("OpenAI API not configured. Please set openai.api.key in application.properties");
        }

        ChatCompletionRequest request = new ChatCompletionRequest();
        request.model = model;
        request.maxTokens = maxTokens;
        request.temperature = temperature;
        request.messages = List.of(
                new ChatMessage("system", systemPrompt),
                new ChatMessage("user", userPrompt)
        );

        log.info("Making OpenAI API request to: {}", "https://api.openai.com/v1/chat/completions");

        return webClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatCompletionResponse.class)
                .doOnNext(response -> {
                    log.info("=== OpenAI Response Received ===");
                    log.info("Response has choices: {}", response.choices != null && !response.choices.isEmpty());
                    if (response.choices != null && !response.choices.isEmpty()) {
                        log.info("Response content length: {}", response.choices.get(0).message.content.length());
                        log.info("Response content preview: {}", response.choices.get(0).message.content.substring(0, Math.min(100, response.choices.get(0).message.content.length())));
                    }
                })
                .map(response -> {
                    if (response.choices != null && !response.choices.isEmpty()) {
                        String content = response.choices.get(0).message.content;
                        log.info("OpenAI API call successful, response length: {}", content.length());
                        return content;
                    }
                    log.warn("No response choices from OpenAI");
                    return "No response from OpenAI";
                })
                .timeout(Duration.ofSeconds(30))
                .doOnError(error -> {
                    log.error("=== OpenAI API Error ===");
                    log.error("Error type: {}", error.getClass().getSimpleName());
                    log.error("Error message: {}", error.getMessage());
                    if (error.getCause() != null) {
                        log.error("Error cause: {}", error.getCause().getMessage());
                    }
                })
                .onErrorReturn("Error communicating with OpenAI API: ");
    }

    // DTOs for OpenAI API
    public static class ChatCompletionRequest {
        public String model;
        public List<ChatMessage> messages;
        @JsonProperty("max_tokens")
        public Integer maxTokens;
        public Double temperature;
    }

    public static class ChatMessage {
        public String role;
        public String content;

        public ChatMessage() {}

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    public static class ChatCompletionResponse {
        public List<Choice> choices;

        public static class Choice {
            public ChatMessage message;
        }
    }
}