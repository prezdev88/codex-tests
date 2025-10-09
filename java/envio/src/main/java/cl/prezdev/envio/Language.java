package cl.prezdev.envio;

import java.util.Locale;

public enum Language {
    ES("es", "Español"),
    EN("en", "English");

    private final String code;
    private final String displayName;

    Language(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String code() {
        return code;
    }

    public String displayName() {
        return displayName;
    }

    public Locale toLocale() {
        return Locale.forLanguageTag(code);
    }

    public static Language fromCode(String code) {
        if (code == null || code.isBlank()) {
            return ES;
        }
        for (Language language : values()) {
            if (language.code.equalsIgnoreCase(code)) {
                return language;
            }
        }
        return ES;
    }
}
