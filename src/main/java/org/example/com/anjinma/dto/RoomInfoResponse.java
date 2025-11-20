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
public class RoomInfoResponse {

    @Schema(description = "방 ID", example = "1")
    private Long roomId;

    @Schema(description = "방 이름", example = "알고리즘")
    private String roomName;

    @Schema(description = "STOMP 엔드포인트", example = "/ws/lecture")
    private String wsEndpoint;

    @Schema(description = "구독 주소", example = "/sub/rooms/1")
    private String subscribeUrl;

    @Schema(description = "발행 주소", example = "/pub/lecture/1")
    private String publishUrl;
}

