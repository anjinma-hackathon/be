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
public class RoomResponse {

    @Schema(description = "방 ID", example = "1")
    private Long roomId;

    @Schema(description = "방 이름", example = "알고리즘")
    private String roomName;

    @Schema(description = "교수 입장 코드(6자리)", example = "ABC123")
    private String professorAuthCode;

    @Schema(description = "학생 입장 코드(6자리)", example = "XYZ789")
    private String studentAuthCode;

    // WebSocket connection info
    @Schema(description = "STOMP 엔드포인트", example = "/ws/lecture")
    private String wsEndpoint;      // WebSocket 연결 엔드포인트

    @Schema(description = "구독 주소", example = "/sub/rooms/1")
    private String subscribeUrl;    // 구독할 주소

    @Schema(description = "발행 주소", example = "/pub/lecture/1")
    private String publishUrl;      // 발행할 주소
}
