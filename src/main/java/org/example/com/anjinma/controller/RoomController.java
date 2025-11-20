package org.example.com.anjinma.controller;

import lombok.RequiredArgsConstructor;
import org.example.com.anjinma.dto.RoomRequest;
import org.example.com.anjinma.dto.RoomResponse;
import org.example.com.anjinma.dto.JoinRoomResponse;
import org.example.com.anjinma.service.RoomService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
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
    public ResponseEntity<RoomResponse> getRoom(@PathVariable Long roomId) {
        RoomResponse response = roomService.getRoomById(roomId);
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
    public ResponseEntity<JoinRoomResponse> joinByCode(@RequestParam("code") String code) {
        JoinRoomResponse response = roomService.joinByCode(code);
        return ResponseEntity.ok(response);
    }
}
