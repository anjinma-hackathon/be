package org.example.com.anjinma.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinRoomResponse {
    private Long roomId;
    private String roomName;
    private String professorAuthCode;
    private String studentAuthCode;
    private Role role; // PROFESSOR or STUDENT

    public enum Role {
        PROFESSOR, STUDENT
    }
}

