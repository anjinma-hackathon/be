package org.example.com.anjinma.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.com.anjinma.dto.RoomRequest;
import org.example.com.anjinma.dto.RoomResponse;
import org.example.com.anjinma.dto.RoomInfoResponse;
import org.example.com.anjinma.dto.JoinRoomResponse;
import org.example.com.anjinma.service.RoomService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
@Tag(name = "Rooms", description = "방 생성/조회 및 코드 입장")
public class RoomController {

    private final RoomService roomService;

    /**
     * Create a new lecture room
     * POST /rooms
     *
     * @param request Room creation request containing roomName
     * @return RoomResponse with roomId, roomName, and professor/student auth codes
     */
    @PostMapping
    @Operation(summary = "방 생성", description = "방을 생성하고 교수/학생 입장 코드를 발급합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = RoomRequest.class),
                            examples = @ExampleObject(name = "createRoom", value = "{\n  \"roomName\": \"알고리즘\"\n}"))))
    @ApiResponse(responseCode = "201", description = "생성됨",
            content = @Content(
                    schema = @Schema(implementation = RoomResponse.class),
                    examples = @ExampleObject(name = "createRoomResponse",
                            value = """
                                    {
                                      "roomId": 1,
                                      "roomName": "알고리즘",
                                      "professorAuthCode": "ABC123",
                                      "studentAuthCode": "XYZ789",
                                      "wsEndpoint": "/ws/lecture",
                                      "subscribeUrl": "/sub/rooms/1",
                                      "publishUrl": "/pub/lecture/1"
                                    }""")))
    public ResponseEntity<RoomResponse> createRoom(@RequestBody RoomRequest request) {
        RoomResponse response = roomService.createRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get room information by room ID
     * GET /rooms/{roomId}
     *
     * @param roomId The room identifier
     * @return RoomResponse with room details
     */
    @GetMapping("/{roomId}")
    @Operation(summary = "방 조회", description = "roomId로 방 정보를 조회합니다.")
    public ResponseEntity<RoomInfoResponse> getRoom(@PathVariable @Parameter(description = "방 ID", example = "1") Long roomId) {
        RoomInfoResponse response = roomService.getRoomById(roomId);
        return ResponseEntity.ok(response);
    }

    /**
     * Join a room by professor/student code.
     * GET /rooms/join?code=XXXXXX
     *
     * @param code Professor or Student code
     * @return JoinRoomResponse containing room info and resolved role
     */
    @GetMapping("/join")
    @Operation(summary = "코드로 입장", description = "교수/학생 코드로 방 입장 시 역할을 판별합니다.")
    public ResponseEntity<JoinRoomResponse> joinByCode(@RequestParam("code") @Parameter(description = "교수 또는 학생 코드", example = "ABC123") String code) {
        JoinRoomResponse response = roomService.joinByCode(code);
        return ResponseEntity.ok(response);
    }
}
