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
public class TranslateResponse {
    @Schema(description = "사용된 언어 코드", example = "en")
    private String lang;
    @Schema(description = "번역 결과")
    private String content;
    @Schema(description = "사용한 모드", example = "chat")
    private String mode;
}

