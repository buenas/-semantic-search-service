package com.semantic_search_service.service.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class OpenAiEmbeddingClient implements EmbeddingClient {

    private static final String OPENAI_EMBEDDINGS_URL = "https://api.openai.com/v1/embeddings";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper;
    private final String apiKey;
    private final String model;

    public OpenAiEmbeddingClient(
            ObjectMapper mapper,
            @Value("${openai.apiKey}") String apiKey,
            @Value("${openai.embeddingModel}") String model) {
        this.mapper = mapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public float[] embed(String text) {
        try {
            HttpResponse<String> response = sendRequest(text != null ? text : "");
            validateResponse(response);
            return parseEmbedding(response.body());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get embedding from OpenAI", e);
        }
    }

    private HttpResponse<String> sendRequest(String text) throws Exception {
        String body = mapper.writeValueAsString(Map.of("model", model, "input", text));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENAI_EMBEDDINGS_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private void validateResponse(HttpResponse<String> response) {
        if (response.statusCode() / 100 != 2) {
            throw new RuntimeException("OpenAI embeddings failed: HTTP "
                    + response.statusCode() + " body=" + response.body());
        }
    }

    private float[] parseEmbedding(String responseBody) throws Exception {
        JsonNode embedding = mapper.readTree(responseBody)
                .path("data").get(0).path("embedding");
        float[] out = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            out[i] = (float) embedding.get(i).asDouble();
        }
        return EmbeddingUtils.l2Normalized(out);
    }
}
