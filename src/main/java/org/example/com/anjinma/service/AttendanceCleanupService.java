package org.example.com.anjinma.service;

import java.time.Duration;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.com.anjinma.dto.StudentListMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceCleanupService {

    private final AttendanceService attendanceService;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${app.attendance.max-idle-seconds:300}")
    private long maxIdleSeconds;

    // 1분마다 idle 세션 정리
    @Scheduled(fixedRate = 60_000)
    public void evictIdle() {
        long maxIdleMillis = Duration.ofSeconds(maxIdleSeconds).toMillis();
        Set<Long> affected = attendanceService.evictIdle(maxIdleMillis);
        for (Long roomId : affected) {
            log.info("Evicted idle students in room {}", roomId);
            StudentListMessage snapshot = StudentListMessage.builder()
                .type("snapshot")
                .students(attendanceService.list(roomId))
                .build();
            messagingTemplate.convertAndSend("/sub/rooms/" + roomId + "/attendance", snapshot);
        }
    }
}

