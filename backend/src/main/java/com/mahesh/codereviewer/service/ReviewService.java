package com.mahesh.codereviewer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mahesh.codereviewer.dto.ReviewRequest;
import com.mahesh.codereviewer.dto.ReviewResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Service
public class ReviewService {

    private static final String SYSTEM_PROMPT = """
            You are a senior software engineer doing a code review.
            Review the code the user sends. Look for bugs, security problems,
            performance issues, bad practices, readability problems and missing error handling.
            Each line of the code is prefixed with its line number and a pipe, for example "12| int x = 0;".

            Respond with ONLY a JSON object, no markdown and no text outside the JSON, in this shape:
            {
              "summary": "2-3 sentence overall assessment",
              "score": <integer 1-10, 10 is excellent>,
              "issues": [
                {
                  "severity": "high" | "medium" | "low",
                  "line": <line number or null if it applies to the whole code>,
                  "title": "short title",
                  "explanation": "why this is a problem",
                  "suggestion": "how to fix it, with a short corrected snippet if useful"
                }
              ]
            }
            Order issues from most to least severe. If the code is good, return few or no issues.
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final int maxTokens;

    public ReviewService(
            ObjectMapper objectMapper,
            @Value("${openrouter.api-key}") String apiKey,
            @Value("${openrouter.model}") String model,
            @Value("${openrouter.base-url}") String baseUrl,
            @Value("${openrouter.max-tokens}") int maxTokens) {
        this.objectMapper = objectMapper;
        this.apiKey = normalizeApiKey(apiKey);
        this.model = model;
        this.maxTokens = maxTokens;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public ReviewResponse review(ReviewRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ReviewException(HttpStatus.SERVICE_UNAVAILABLE,
                    "OPENROUTER_API_KEY is not set on the server.");
        }

        String language = (request.language() == null || request.language().isBlank())
                ? "unknown" : request.language();
        String userMessage = "Language: " + language + "\n\nCode:\n" + addLineNumbers(request.code());

        Map<String, Object> body = Map.of(
            "model", model,
            "max_tokens", maxTokens,
            "response_format", Map.of("type", "json_object"),
            "messages", List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", userMessage))
        );

        JsonNode apiResponse;
        try {
            apiResponse = restClient.post()
                    .uri("/api/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException e) {
            throw new ReviewException(HttpStatus.BAD_GATEWAY,
                    "OpenRouter API returned " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new ReviewException(HttpStatus.BAD_GATEWAY,
                    "Could not reach the OpenRouter API: " + e.getMessage());
        }

        String text = extractText(apiResponse);
        return parseReview(text);
    }

    private String addLineNumbers(String code) {
        String[] lines = code.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            sb.append(i + 1).append("| ").append(lines[i]).append('\n');
        }
        return sb.toString();
    }

    private String normalizeApiKey(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() >= 2
                && ((normalized.startsWith("\"") && normalized.endsWith("\""))
                || (normalized.startsWith("'") && normalized.endsWith("'")))) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        return normalized;
    }

    private String extractText(JsonNode apiResponse) {
        JsonNode content = apiResponse == null ? null : apiResponse.path("choices").path(0)
                .path("message").path("content");
        if (content == null || !content.isTextual()) {
            throw new ReviewException(HttpStatus.BAD_GATEWAY, "Empty response from OpenRouter API.");
        }
        return content.asText();
    }

    private ReviewResponse parseReview(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new ReviewException(HttpStatus.BAD_GATEWAY, "OpenRouter did not return valid JSON.");
        }
        try {
            ReviewResponse parsed = objectMapper.readValue(text.substring(start, end + 1), ReviewResponse.class);
            return new ReviewResponse(
                    parsed.summary(),
                    Math.max(1, Math.min(10, parsed.score())),
                    parsed.issues() == null ? List.of() : parsed.issues());
        } catch (Exception e) {
            throw new ReviewException(HttpStatus.BAD_GATEWAY,
                    "Could not parse OpenRouter's review: " + e.getMessage());
        }
    }
}
