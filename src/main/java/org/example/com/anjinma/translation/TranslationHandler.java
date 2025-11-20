package org.example.com.anjinma.translation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class TranslationHandler implements WebSocketHandler {

    private final OllamaService ollamaService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        Flux<String> input = session.receive()
            .map(WebSocketMessage::getPayloadAsText)
            .flatMap(payload -> {
                String t = extractTextFieldSafely(payload);
                return (t == null || t.isBlank()) ? Mono.empty() : Mono.just(t);
            });

        Flux<WebSocketMessage> out = input.flatMap(text -> {
            // Build 5 language streams (ko 제외): EN, JA, ZH, VI, ES
            Flux<String> en = ollamaService.fetchTranslationStream(text, Language.EN);
            Flux<String> ja = ollamaService.fetchTranslationStream(text, Language.JA);
            Flux<String> zh = ollamaService.fetchTranslationStream(text, Language.ZH);
            Flux<String> vi = ollamaService.fetchTranslationStream(text, Language.VI);
            Flux<String> es = ollamaService.fetchTranslationStream(text, Language.ES);

            // Merge concurrently (no sequential processing)
            return Flux.merge(
                    en.map(c -> new LangChunk(Language.EN.code(), c)),
                    ja.map(c -> new LangChunk(Language.JA.code(), c)),
                    zh.map(c -> new LangChunk(Language.ZH.code(), c)),
                    vi.map(c -> new LangChunk(Language.VI.code(), c)),
                    es.map(c -> new LangChunk(Language.ES.code(), c))
                )
                .map(this::toProtocolJson)
                .map(session::textMessage)
                .onErrorResume(ex -> Flux.empty());
        });

        return session.send(out);
    }

    private String extractTextFieldSafely(String payload) {
        try {
            // Expecting either raw text or JSON {"text":"..."}
            if (payload == null || payload.isBlank()) return null;
            if (payload.trim().startsWith("{")) {
                Map<?, ?> map = objectMapper.readValue(payload, Map.class);
                Object t = map.get("text");
                return t == null ? null : String.valueOf(t);
            }
            return payload;
        } catch (Exception e) {
            return null;
        }
    }

    private String toProtocolJson(LangChunk chunk) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                "type", "chunk",
                "lang", chunk.lang,
                "content", chunk.content
            ));
        } catch (JsonProcessingException e) {
            // Fallback plain
            return "{\"type\":\"chunk\",\"lang\":\"" + chunk.lang + "\",\"content\":\"" + escape(chunk.content) + "\"}";
        }
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record LangChunk(String lang, String content) {}
}
