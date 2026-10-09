package com.expense_tracker.service.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class GeminiClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final String baseUrl;

    public GeminiClient(
            @Value("${gemini.api.key}") String apiKey,
            @Value("${gemini.model:gemini-3.8-flash}") String model,
            @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = model;
        this.baseUrl = baseUrl;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * Calls Google Gemini API v1beta :generateContent with contents and tool declarations.
     */
    public Map<String, Object> generateContent(List<Map<String, Object>> contents, List<Map<String, Object>> tools) {
        if (apiKey.isBlank() || "demo-api-key".equalsIgnoreCase(apiKey)) {
            throw new IllegalStateException("GEMINI_API_KEY is not configured (currently using placeholder 'demo-api-key'). Configure a valid Google Gemini API key to use live generative AI.");
        }

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("contents", contents);

        if (tools != null && !tools.isEmpty()) {
            requestBody.put("tools", tools);
        }

        // Configure generation parameters
        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("temperature", 0.2);
        generationConfig.put("maxOutputTokens", 1024);
        requestBody.put("generationConfig", generationConfig);

        String endpointUri = String.format("/models/%s:generateContent?key=%s", model, apiKey);

        try {
            log.debug("Dispatching request to Gemini API endpoint: /models/{}:generateContent", model);
            String rawResponse = restClient.post()
                    .uri(endpointUri)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            if (rawResponse == null || rawResponse.isBlank()) {
                return Map.of("error", "Empty response received from Gemini API");
            }

            return objectMapper.readValue(rawResponse, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Gemini API call unsuccessful: {}. Check that GEMINI_API_KEY is valid and has Gemini API enabled.", e.getMessage());
            throw new RuntimeException("Gemini AI API communication error: " + e.getMessage(), e);
        }
    }
}
