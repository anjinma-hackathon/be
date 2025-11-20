package org.example.com.anjinma.service;

import java.awt.image.BufferedImage;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OcrService {

    private final ITesseract tess;

    public OcrService() {
        this.tess = new Tesseract();
        // You can set datapath via env TESSDATA_PREFIX or here.
        // tess.setDatapath("/usr/share/tesseract-ocr/4.00/tessdata");
        // tess.setLanguage("kor+eng+jpn+chi_sim+vie+spa"); // enable multiple as needed
    }

    public String ocr(BufferedImage image, String langCode) {
        try {
            if (langCode != null && !langCode.isBlank()) {
                String l = mapLang(langCode);
                if (l != null) tess.setLanguage(l);
            }
            return tess.doOCR(image);
        } catch (TesseractException e) {
            log.warn("OCR failed: {}", e.getMessage());
            return "";
        }
    }

    private String mapLang(String code) {
        return switch (code.toLowerCase()) {
            case "ko" -> "kor";
            case "en" -> "eng";
            case "ja" -> "jpn";
            case "zh" -> "chi_sim";
            case "vi" -> "vie";
            case "es" -> "spa";
            default -> null;
        };
    }
}

