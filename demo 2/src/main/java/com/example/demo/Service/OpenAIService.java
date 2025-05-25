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

        return webClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatCompletionResponse.class)
                .map(response -> {
                    if (response.choices != null && !response.choices.isEmpty()) {
                        return response.choices.get(0).message.content;
                    }
                    return "No response from OpenAI";
                })
                .timeout(Duration.ofSeconds(30))
                .onErrorReturn("Error communicating with OpenAI API");
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