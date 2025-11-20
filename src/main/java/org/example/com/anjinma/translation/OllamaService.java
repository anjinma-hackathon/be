package org.example.com.anjinma.translation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class OllamaService {

    private static final String OLLAMA_URL = "https://vortex.bluerack.org/api/chat";
    private static final String MODEL = "exaone3.5";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebClient webClient = WebClient.builder()
        .baseUrl(OLLAMA_URL)
        .build();

    /**
     * Fetch streaming translation from Ollama-like API as NDJSON.
     * Returns an empty Flux on any error.
     */
    public Flux<String> fetchTranslationStream(String text, Language lang) {
        try {
            Map<String, Object> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", lang.systemPrompt());

            Map<String, Object> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", text);

            Map<String, Object> payload = new HashMap<>();
            payload.put("model", MODEL);
            payload.put("messages", List.of(systemMsg, userMsg));
            payload.put("stream", true);

            return webClient
                .post()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_NDJSON, MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(payload))
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap(line -> {
                    String content = extractContentSafely(line);
                    return (content == null || content.isEmpty()) ? Flux.empty() : Flux.just(content);
                })
                .timeout(Duration.ofSeconds(60))
                .onErrorResume(ex -> Flux.empty());
        } catch (Exception e) {
            return Flux.empty();
        }
    }

    private String extractContentSafely(String line) {
        try {
            if (line == null || line.isBlank()) return null;
            JsonNode root = objectMapper.readTree(line);
            JsonNode msg = root.path("message");
            JsonNode content = msg.path("content");
            if (content.isMissingNode() || content.isNull()) return null;
            return content.asText();
        } catch (JsonProcessingException e) {
            return null; // ignore malformed chunk
        }
    }
}
