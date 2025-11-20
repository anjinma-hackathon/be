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
    private String professorId;

    public Room toEntity(String authCode) {
        return Room.builder()
            .roomName(roomName)
            .professorId(professorId)
            .authCode(authCode)
            .build();
    }
}
