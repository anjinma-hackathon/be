package org.example.com.anjinma.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.com.anjinma.entity.Room;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomRequest {
    private String roomName;
    // 교수/학생 인증 코드는 서버에서 생성

    public Room toEntity(String professorAuthCode, String studentAuthCode) {
        return Room.builder()
            .roomName(roomName)
            .professorAuthCode(professorAuthCode)
            .studentAuthCode(studentAuthCode)
            .build();
    }
}
