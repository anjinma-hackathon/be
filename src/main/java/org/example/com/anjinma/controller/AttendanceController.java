package org.example.com.anjinma.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.com.anjinma.dto.StudentJoinMessage;
import org.example.com.anjinma.dto.StudentListMessage;
import org.example.com.anjinma.service.AttendanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
@Tag(name = "Attendance", description = "교수 화면에서 현재 입장 학생 목록 조회 (디버깅/프론트 편의)")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping("/{roomId}/attendance")
    @Operation(summary = "현재 입장 학생 목록",
        description = "웹소켓 없이도 현재 입장한 학생 목록을 스냅샷으로 조회합니다.",
        responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            content = @Content(schema = @Schema(implementation = StudentListMessage.class))
        ))
    public ResponseEntity<StudentListMessage> list(@PathVariable @Parameter(description = "방 ID", example = "1") Long roomId) {
        List<StudentJoinMessage> students = attendanceService.list(roomId);
        return ResponseEntity.ok(StudentListMessage.builder().type("snapshot").students(students).build());
    }
}

