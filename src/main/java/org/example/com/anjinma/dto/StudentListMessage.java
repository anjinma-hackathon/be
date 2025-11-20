package org.example.com.anjinma.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentListMessage {
    @Schema(description = "메시지 타입", example = "snapshot")
    private String type; // "snapshot"

    @ArraySchema(schema = @Schema(implementation = StudentJoinMessage.class))
    private List<StudentJoinMessage> students;
}
