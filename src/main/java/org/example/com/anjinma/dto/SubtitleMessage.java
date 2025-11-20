package org.example.com.anjinma.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubtitleMessage {
    private String sourceLanguage;
    private String targetLanguage;
    private String originalText;
    private String translatedText;
}
