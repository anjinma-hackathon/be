package org.example.com.anjinma.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TranslateRequest {
    @Schema(description = "번역 대상 언어 코드(ko,en,ja,zh,vi,es)", example = "en")
    private String language;
    @Schema(description = "번역할 텍스트 청크", example = "첫째 줄\n둘째 줄")
    private String text;
    @Schema(description = "엔드포인트 선택: generate | chat", example = "chat")
    private String mode;
}
