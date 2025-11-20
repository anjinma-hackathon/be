package org.example.com.anjinma.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.com.anjinma.dto.StudentJoinMessage;
import org.example.com.anjinma.dto.StudentListMessage;
import org.example.com.anjinma.service.AttendanceService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AttendanceHandler {

    private final SimpMessagingTemplate messagingTemplate;
    private final AttendanceService attendanceService;

    /**
     * 학생 입장: /pub/attendance/{roomId}
     * 방송: /sub/rooms/{roomId}/attendance (현재 참석자 전체 스냅샷)
     */
    @MessageMapping("/attendance/{roomId}")
    public void handleStudentJoin(@DestinationVariable Long roomId,
                                  @org.springframework.messaging.handler.annotation.Header("simpSessionId") String sessionId,
                                  StudentJoinMessage message) {
        if (message == null || message.getStudentId() == null || message.getStudentName() == null) {
            return;
        }
        attendanceService.join(roomId, message, sessionId);
        log.info("Student joined room {}: [{}] {}", roomId, message.getStudentId(), message.getStudentName());
        broadcastSnapshot(roomId);
    }

    /**
     * 학생 퇴장: /pub/attendance/{roomId}/leave
     */
    @MessageMapping("/attendance/{roomId}/leave")
    public void handleStudentLeave(@DestinationVariable Long roomId, StudentJoinMessage message) {
        if (message == null || message.getStudentId() == null) {
            return;
        }
        attendanceService.leave(roomId, message.getStudentId());
        log.info("Student left room {}: [{}]", roomId, message.getStudentId());
        broadcastSnapshot(roomId);
    }

    /**
     * 교수 측 동기화 요청: /pub/attendance/{roomId}/sync
     */
    @MessageMapping("/attendance/{roomId}/sync")
    public void handleSync(@DestinationVariable Long roomId) {
        broadcastSnapshot(roomId);
    }

    /**
     * 학생 keep-alive: /pub/attendance/{roomId}/ping
     */
    @MessageMapping("/attendance/{roomId}/ping")
    public void handlePing(@DestinationVariable Long roomId,
                           @org.springframework.messaging.handler.annotation.Header("simpSessionId") String sessionId) {
        // roomId는 경로 바인딩을 위해 필요하며, 로그/추적에 활용할 수 있다.
        if (roomId == null) {
            return;
        }
        attendanceService.touch(sessionId);
    }

    private void broadcastSnapshot(Long roomId) {
        StudentListMessage snapshot = StudentListMessage.builder()
            .type("snapshot")
            .students(attendanceService.list(roomId))
            .build();
        messagingTemplate.convertAndSend("/sub/rooms/" + roomId + "/attendance", snapshot);
    }
}
