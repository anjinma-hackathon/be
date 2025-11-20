package org.example.com.anjinma.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentJoinMessage {
    @Schema(description = "학번", example = "20231234")
    private String studentId;
    @Schema(description = "이름", example = "홍길동")
    private String studentName;
}
