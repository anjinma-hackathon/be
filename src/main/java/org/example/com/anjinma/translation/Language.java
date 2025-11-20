package org.example.com.anjinma.translation;

import java.util.Arrays;

public enum Language {
    KO("ko", "Korean", "Echo input as-is."),
    EN("en", "English", "Translate to English. Output only the translated text."),
    JA("ja", "Japanese", "Translate to Japanese. Output only the translated text."),
    ZH("zh", "Chinese", "Translate to Chinese. Output only the translated text."),
    VI("vi", "Vietnamese", "Translate to Vietnamese. Output only the translated text."),
    ES("es", "Spanish", "Translate to Spanish. Output only the translated text.");

    private final String code;
    private final String displayName;
    private final String systemPrompt;

    Language(String code, String displayName, String systemPrompt) {
        this.code = code;
        this.displayName = displayName;
        this.systemPrompt = systemPrompt;
    }

    public String code() {
        return code;
    }

    public String displayName() {
        return displayName;
    }

    public String systemPrompt() {
        return systemPrompt;
    }

    public static Language fromCode(String code) {
        return Arrays.stream(values())
            .filter(l -> l.code.equalsIgnoreCase(code))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unsupported language code: " + code));
    }
}

