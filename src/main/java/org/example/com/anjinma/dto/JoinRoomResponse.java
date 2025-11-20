package org.example.com.anjinma.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinRoomResponse {

    @Schema(description = "방 ID", example = "1")
    private Long roomId;

    @Schema(description = "방 이름", example = "알고리즘")
    private String roomName;

    @Schema(description = "교수 코드", example = "ABC123")
    private String professorAuthCode;

    @Schema(description = "학생 코드", example = "XYZ789")
    private String studentAuthCode;

    @Schema(description = "역할", example = "PROFESSOR", allowableValues = {"PROFESSOR","STUDENT"})
    private Role role; // PROFESSOR or STUDENT

    public enum Role {
        PROFESSOR, STUDENT
    }
}
