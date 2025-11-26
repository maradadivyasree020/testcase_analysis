package com.example.tools.llm;

import com.example.tools.llm.dto.LlmRequest;
import com.example.tools.llm.dto.LlmResponse;
import org.springframework.web.reactive.function.client.WebClient;

public class LlmClient {

    private final WebClient webClient;
    private final String model;

    public LlmClient(String baseUrl, String apiKey, String model) {
        this.model = model;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    public LlmResponse callLlm(LlmRequest request) {
        return webClient.post()
                .uri("/v1/chat/completions") // adjust if provider differs
                .bodyValue(request)
                .retrieve()
                .bodyToMono(LlmResponse.class)
                .block();
    }

    public String getModel() {
        return model;
    }

    public static LlmRequest.Message system(String content) {
        return new LlmRequest.Message("system", content);
    }

    public static LlmRequest.Message user(String content) {
        return new LlmRequest.Message("user", content);
    }
}
