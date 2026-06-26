package com.fitness.aiservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class HuggingFaceService {

    private final WebClient webClient;

    @Value("${huggingface.api.url}")
    private String apiUrl;

    @Value("${huggingface.api.key}")
    private String apiKey;

    public HuggingFaceService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public String getAnswer(String question) {
        Map<String, Object> requestBody = Map.of(
                "inputs", question,
                "parameters", Map.of(
                        "max_new_tokens", 1024,
                        "return_full_text", false
                )
        );

        return webClient.post()
                .uri(apiUrl)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
