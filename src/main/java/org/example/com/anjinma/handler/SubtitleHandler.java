package org.example.com.anjinma.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.com.anjinma.dto.SubtitleMessage;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class SubtitleHandler {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handle incoming subtitle messages from professor Endpoint: /pub/lecture/{roomId}
     *
     * @param roomId  The room identifier
     * @param message The subtitle message containing original text and language info
     */
    @MessageMapping("/lecture/{roomId}")
    public void handleSubtitle(@DestinationVariable Long roomId, SubtitleMessage message) {
        log.info("Received subtitle for room {}: {}", roomId, message.getOriginalText());

        // TODO: Implement translation logic here
        // For now, we'll use a placeholder for the translated text
        // In production, this should call a translation service (e.g., Google Translate API, DeepL, etc.)

        // Temporary: Set translated text to be the same as original text
        // Replace this with actual translation service call
        message.setTranslatedText("[TRANSLATED] " + message.getOriginalText());

        // Broadcast the subtitle message to all subscribers of this room
        // Students subscribed to /sub/rooms/{roomId} will receive this message\

        //그러면 여기서 message를 번역해서 주면 되겠다.

        messagingTemplate.convertAndSend("/sub/rooms/" + roomId, message);

        log.info("Broadcasted subtitle to room {}", roomId);
    }
}
