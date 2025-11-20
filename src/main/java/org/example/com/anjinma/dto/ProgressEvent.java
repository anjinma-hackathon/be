package org.example.com.anjinma.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressEvent {
    private String type;      // started, ocr_started, ocr_page, translate_page, overlay, completed, error
    private String message;   // human readable
    private Integer current;  // current index (1-based) if applicable
    private Integer total;    // total pages if applicable
}

