package org.example.com.anjinma.handler;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.com.anjinma.dto.StudentJoinMessage;
import org.example.com.anjinma.dto.SubtitleMessage;
import org.example.com.anjinma.service.AttendanceService;
import org.example.com.anjinma.translation.Language;
import org.example.com.anjinma.translation.OllamaService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

@Slf4j
@Controller
@RequiredArgsConstructor
public class SubtitleHandler {

    private final SimpMessagingTemplate messagingTemplate;
    private final AttendanceService attendanceService;
    private final OllamaService ollamaService;

    /**
     * Handle incoming subtitle messages from professor Endpoint: /pub/lecture/{roomId}
     *
     * @param roomId  The room identifier
     * @param message The subtitle message containing original text and language info
     */
    @MessageMapping("/lecture/{roomId}")
    public void handleSubtitle(@DestinationVariable Long roomId, SubtitleMessage message) {
        String original = message == null ? null : message.getOriginalText();
        if (original == null || original.isBlank()) {
            return;
        }

        log.info("Received subtitle for room {}: {}", roomId, original);

        // 1) 원문 브로드캐스트(교수/공용 보기용). 번역은 별도 언어별 메시지로 팬아웃.
        SubtitleMessage originalMsg = new SubtitleMessage(
            message.getSourceLanguage(),
            null,
            original,
            null
        );
        messagingTemplate.convertAndSend("/sub/rooms/" + roomId, originalMsg);

        // 2) 현재 방에 있는 학생들의 언어 수집
        List<StudentJoinMessage> students = attendanceService.list(roomId);
        Set<String> languageCodes = students.stream()
            .map(StudentJoinMessage::getLanguage)
            .filter(code -> code != null && !code.isBlank())
            .map(String::trim)
            .map(String::toLowerCase)
            .filter(code -> !"ko".equals(code))
            .collect(Collectors.toSet());

        if (languageCodes.isEmpty()) {
            log.debug("No target languages present in room {}. Skipping translation.", roomId);
            return;
        }

        // 3) 언어별 번역 스트림 수행 후, chunk 단위로 targetLanguage를 세팅하여 공용 채널로 브로드캐스트
        for (String code : languageCodes) {
            Language lang;
            try {
                lang = Language.fromCode(code);
            } catch (IllegalArgumentException e) {
                log.debug("Unsupported language code {} in room {} — skipping.", code, roomId);
                continue;
            }

            Flux<String> stream = ollamaService.fetchTranslationStream(original, lang);
            stream.subscribe(
                chunk -> {
                    if (chunk == null || chunk.isBlank()) return;
                    SubtitleMessage translatedChunk = new SubtitleMessage(
                        message.getSourceLanguage(),
                        lang.code(),
                        original,
                        chunk
                    );
                    messagingTemplate.convertAndSend("/sub/rooms/" + roomId, translatedChunk);
                },
                ex -> log.warn("Translation stream failed for room {} lang {}: {}", roomId, code, ex.toString())
            );
        }

        log.info("Started translation streams for room {} languages: {}", roomId, languageCodes);
    }
}
