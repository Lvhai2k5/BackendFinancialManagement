package ute.fit.financemanagement.service.impl;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ute.fit.financemanagement.exception.ApiException;
import ute.fit.financemanagement.service.GeminiClient;

/** Gọi REST `generateContent` của Gemini. API key chỉ nằm ở Backend (biến môi trường GEMINI_API_KEY). */
@Service
public class GeminiClientImpl implements GeminiClient {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String PARSE_FAILED = "Không phân tích được câu nói, vui lòng nhập thủ công.";

    private static final Logger log = LoggerFactory.getLogger(GeminiClientImpl.class);
    private static final int MAX_ATTEMPTS = 2;

    private final String apiKey;
    private final String model;
    private final String thinkingLevel;
    private final RestClient restClient;

    public GeminiClientImpl(@Value("${app.gemini.api-key:}") String apiKey,
                            @Value("${app.gemini.model:gemini-3.5-flash}") String model,
                            @Value("${app.gemini.thinking-level:low}") String thinkingLevel,
                            @Value("${app.gemini.timeout-seconds:20}") int timeoutSeconds) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.thinkingLevel = thinkingLevel == null ? "" : thinkingLevel.trim();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public String generateJson(String systemPrompt, String userText, Map<String, Object> jsonSchema) {
        if (apiKey.isEmpty()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Ghi chi tiêu bằng giọng nói chưa được cấu hình trên máy chủ.");
        }
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0);
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("responseSchema", jsonSchema);
        if (!thinkingLevel.isEmpty()) {
            // Model "suy nghĩ" mặc định mất 25s+; mức thấp đủ cho việc trích xuất đơn giản này.
            generationConfig.put("thinkingConfig", Map.of("thinkingLevel", thinkingLevel));
        }
        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))),
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", userText)))),
                "generationConfig", generationConfig);

        for (int attempt = 1; ; attempt++) {
            try {
                Map<String, Object> response = restClient.post()
                        .uri(BASE_URL + model + ":generateContent")
                        .header("x-goog-api-key", apiKey)
                        .body(body)
                        .retrieve()
                        .body(new ParameterizedTypeReference<>() {
                        });
                return extractText(response);
            } catch (HttpServerErrorException e) {
                // 503 "high demand" của Google thường qua nhanh: thử lại 1 lần rồi mới báo lỗi.
                log.warn("Gemini {} lỗi {} (lần {}/{}): {}", model, e.getStatusCode().value(), attempt,
                        MAX_ATTEMPTS, abbreviate(e.getResponseBodyAsString()));
                if (attempt < MAX_ATTEMPTS && e.getStatusCode().value() == 503) {
                    continue;
                }
                throw new ApiException(HttpStatus.BAD_GATEWAY,
                        "Dịch vụ nhận diện đang quá tải, vui lòng thử lại sau ít phút hoặc nhập thủ công.");
            } catch (HttpClientErrorException e) {
                log.warn("Gemini {} lỗi {}: {}", model, e.getStatusCode().value(),
                        abbreviate(e.getResponseBodyAsString()));
                if (e.getStatusCode().value() == 429) {
                    throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                            "Dịch vụ nhận diện đang quá tải, vui lòng thử lại sau ít phút.");
                }
                throw new ApiException(HttpStatus.BAD_GATEWAY, PARSE_FAILED);
            } catch (RestClientException e) {
                log.warn("Gemini {} không kết nối được: {}", model, e.getMessage());
                throw new ApiException(HttpStatus.BAD_GATEWAY,
                        "Không kết nối được dịch vụ nhận diện, vui lòng thử lại hoặc nhập thủ công.");
            }
        }
    }

    private static String abbreviate(String text) {
        if (text == null) {
            return "";
        }
        String oneLine = text.replaceAll("\\s+", " ").trim();
        return oneLine.length() > 300 ? oneLine.substring(0, 300) + "..." : oneLine;
    }

    /** candidates[0].content.parts[0].text — thiếu/rỗng thì coi như model không trả được kết quả. */
    @SuppressWarnings("unchecked")
    private static String extractText(Map<String, Object> response) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            Object text = parts.get(0).get("text");
            if (text instanceof String s && !s.isBlank()) {
                return s;
            }
        } catch (RuntimeException ignored) {
            // rơi xuống lỗi chung bên dưới
        }
        throw new ApiException(HttpStatus.BAD_GATEWAY, PARSE_FAILED);
    }
}
