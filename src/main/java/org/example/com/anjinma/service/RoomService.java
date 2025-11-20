package org.example.com.anjinma.service;

import java.security.SecureRandom;
import lombok.RequiredArgsConstructor;
import org.example.com.anjinma.dto.RoomRequest;
import org.example.com.anjinma.dto.RoomResponse;
import org.example.com.anjinma.dto.RoomInfoResponse;
import org.example.com.anjinma.dto.JoinRoomResponse;
import org.example.com.anjinma.entity.Room;
import org.example.com.anjinma.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private static final String AUTH_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int AUTH_CODE_LENGTH = 6;
    private static final SecureRandom random = new SecureRandom();

    @Transactional
    public RoomResponse createRoom(RoomRequest request) {
        // Generate unique 6-digit codes for professor and student
        String professorAuthCode = generateUniqueProfessorCode();
        String studentAuthCode = generateUniqueStudentCode();

        // Ensure professor and student codes are not identical for the same room
        while (studentAuthCode.equals(professorAuthCode)) {
            studentAuthCode = generateUniqueStudentCode();
        }

        // Create and save room entity
        Room room = request.toEntity(professorAuthCode, studentAuthCode);
        Room savedRoom = roomRepository.save(room);

        // Build response with WebSocket connection info
        return RoomResponse.builder()
            .roomId(savedRoom.getId())
            .roomName(savedRoom.getRoomName())
            .professorAuthCode(savedRoom.getProfessorAuthCode())
            .studentAuthCode(savedRoom.getStudentAuthCode())
            .wsEndpoint("/ws/lecture")
            .subscribeUrl("/sub/rooms/" + savedRoom.getId())
            .publishUrl("/pub/lecture/" + savedRoom.getId())
            .build();
    }

    @Transactional(readOnly = true)
    public RoomInfoResponse getRoomById(Long roomId) {
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new RuntimeException("Room not found with id: " + roomId));

        return RoomInfoResponse.builder()
            .roomId(room.getId())
            .roomName(room.getRoomName())
            .wsEndpoint("/ws/lecture")
            .subscribeUrl("/sub/rooms/" + room.getId())
            .publishUrl("/pub/lecture/" + room.getId())
            .build();
    }

    @Transactional(readOnly = true)
    public JoinRoomResponse joinByCode(String code) {
        return roomRepository.findByProfessorAuthCode(code)
            .map(room -> JoinRoomResponse.builder()
                .roomId(room.getId())
                .roomName(room.getRoomName())
                .role(JoinRoomResponse.Role.PROFESSOR)
                .build())
            .or(() -> roomRepository.findByStudentAuthCode(code)
                .map(room -> JoinRoomResponse.builder()
                    .roomId(room.getId())
                    .roomName(room.getRoomName())
                    .role(JoinRoomResponse.Role.STUDENT)
                    .build()))
            .orElseThrow(() -> new RuntimeException("Invalid room code: " + code));
    }

    private String generateAuthCode() {
        StringBuilder sb = new StringBuilder(AUTH_CODE_LENGTH);
        for (int i = 0; i < AUTH_CODE_LENGTH; i++) {
            sb.append(AUTH_CODE_CHARS.charAt(random.nextInt(AUTH_CODE_CHARS.length())));
        }
        return sb.toString();
    }

    private String generateUniqueProfessorCode() {
        String code = generateAuthCode();
        while (roomRepository.existsByProfessorAuthCode(code)) {
            code = generateAuthCode();
        }
        return code;
    }

    private String generateUniqueStudentCode() {
        String code = generateAuthCode();
        while (roomRepository.existsByStudentAuthCode(code)) {
            code = generateAuthCode();
        }
        return code;
    }
}
