package org.example.com.anjinma.service;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.com.anjinma.repository.RoomRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomCleanupService {

    private final RoomRepository roomRepository;

    // 매일 새벽 3시 정각에 실행 (서버 로컬 시간 기준)
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deleteRoomsOlderThanOneYear() {
        LocalDateTime cutoff = LocalDateTime.now().minusYears(1);
        long deleted = roomRepository.deleteByCreatedAtBefore(cutoff);
        if (deleted > 0) {
            log.info("Deleted {} room(s) older than {}", deleted, cutoff);
        }
    }
}

