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
        try {
            return webClient.post()
                    .uri("/v1/chat/completions") // adjust if provider differs
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(
                        status -> !status.is2xxSuccessful(),
                        response -> {
                            return response.bodyToMono(String.class).map(body ->
                                new RuntimeException(
                                    "LLM API HTTP error (likely 405 Method Not Allowed)" +
                                    "\nEndpoint attempted: /v1/chat/completions" +
                                    "\nVerify LLM_BASE_URL is correct (base URL without /v1/chat/completions)" +
                                    "\nResponse: " + body
                                )
                            );
                        }
                    )
                    .bodyToMono(LlmResponse.class)
                    .block();
        } catch (Exception e) {
            String msg = "LLM API call failed: " + e.getMessage() +
                         "\nCheck that LLM_BASE_URL and LLM_API_KEY are correctly configured.";
            throw new RuntimeException(msg, e);
        }
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
