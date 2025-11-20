package org.example.com.anjinma.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponse {
    private Long roomId;
    private String roomName;
    private String authCode;

    // WebSocket connection info
    private String wsEndpoint;      // WebSocket 연결 엔드포인트: "/ws/lecture"
    private String subscribeUrl;    // 구독할 주소: "/sub/rooms/{roomId}"
    private String publishUrl;      // 발행할 주소: "/pub/lecture/{roomId}"
}
