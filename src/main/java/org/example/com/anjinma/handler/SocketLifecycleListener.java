package org.example.com.anjinma.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.com.anjinma.dto.StudentListMessage;
import org.example.com.anjinma.service.AttendanceService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class SocketLifecycleListener {

    private final AttendanceService attendanceService;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        Long roomId = attendanceService.leaveBySession(sessionId);
        if (roomId != null) {
            log.info("Session {} disconnected; removed from room {}", sessionId, roomId);
            StudentListMessage snapshot = StudentListMessage.builder()
                .type("snapshot")
                .students(attendanceService.list(roomId))
                .build();
            messagingTemplate.convertAndSend("/sub/rooms/" + roomId + "/attendance", snapshot);
        }
    }
}

