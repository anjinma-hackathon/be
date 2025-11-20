package org.example.com.anjinma.repository;

import org.example.com.anjinma.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    boolean existsByProfessorAuthCode(String professorAuthCode);
    boolean existsByStudentAuthCode(String studentAuthCode);
    java.util.Optional<Room> findByProfessorAuthCode(String professorAuthCode);
    java.util.Optional<Room> findByStudentAuthCode(String studentAuthCode);

    long deleteByCreatedAtBefore(LocalDateTime cutoff);
}
