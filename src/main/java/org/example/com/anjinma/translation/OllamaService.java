package org.example.com.anjinma.translation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

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

    // -------- Document translation (non-streaming) helpers --------

    public Mono<String> translateWithGenerate(String text, Language lang) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", MODEL);
        payload.put("stream", false);
        payload.put("prompt", buildGeneratePrompt(text, lang));

        return webClient.post()
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(payload))
            .retrieve()
            .bodyToMono(String.class)
            .flatMap(this::parseNonStreamingBody)
            .retryWhen(Retry.backoff(3, Duration.ofMillis(300))
                .filter(ex -> !(ex instanceof TimeoutException)))
            .onErrorResume(ex -> Mono.just(""));
    }

    public Mono<String> translateWithChat(String text, Language lang) {
        Map<String, Object> systemMsg = Map.of("role", "system", "content", buildChatSystem(lang));
        Map<String, Object> userMsg = Map.of("role", "user", "content", text);
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", MODEL);
        payload.put("stream", false);
        payload.put("messages", List.of(systemMsg, userMsg));

        return webClient.post()
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(payload))
            .retrieve()
            .bodyToMono(String.class)
            .flatMap(this::parseNonStreamingBody)
            .retryWhen(Retry.backoff(3, Duration.ofMillis(300))
                .filter(ex -> !(ex instanceof TimeoutException)))
            .onErrorResume(ex -> Mono.just(""));
    }

    private String buildGeneratePrompt(String text, Language lang) {
        return "다음 텍스트를 " + lang.displayName() + "로 번역하세요. 규칙:\n" +
            "1) 번역된 텍스트만 출력\n" +
            "2) 원문과 동일한 줄바꿈/빈 줄/문단 구분 유지\n" +
            "3) 문장 병합/분할 금지, 불필요한 추가 텍스트 금지\n" +
            "4) 괄호/번호/기호 같은 인라인 서식 최대한 보존\n\n" +
            "--- 텍스트 시작 ---\n" + text + "\n--- 텍스트 끝 ---";
    }

    private String buildChatSystem(Language lang) {
        return "당신은 전문 문서 번역가입니다. 규칙:\n" +
            "1) 제공된 텍스트를 " + lang.displayName() + "로 번역\n" +
            "2) 번역된 텍스트만 출력\n" +
            "3) 원문의 줄바꿈/빈 줄/문단 구분을 그대로 보존\n" +
            "4) 문장 병합/분할 금지, 추가 설명 금지\n" +
            "5) 괄호/번호/기호 등 인라인 서식 최대한 보존";
    }

    private Mono<String> parseNonStreamingBody(String raw) {
        try {
            JsonNode root = objectMapper.readTree(raw);
            // try chat format first
            JsonNode msg = root.path("message");
            JsonNode content = msg.path("content");
            if (!content.isMissingNode() && !content.isNull()) {
                return Mono.just(content.asText());
            }
            // try generate format: common providers use 'response'
            JsonNode resp = root.path("response");
            if (!resp.isMissingNode() && !resp.isNull()) {
                return Mono.just(resp.asText());
            }
            // fallback: unknown schema -> return raw
            return Mono.just(raw);
        } catch (Exception e) {
            return Mono.just(raw);
        }
    }
}
