package cl.prezdev.envio;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Settings {
    @Setter
    private float uiScale = 1.0f;
    @Setter
    private int windowWidth = -1;
    @Setter
    private int windowHeight = -1;
    private String language = Language.ES.code();
    private final Map<String, Float> componentScales = new LinkedHashMap<>();

    public Settings() {
    }

    public static Settings defaults() {
        Settings settings = new Settings();
        settings.uiScale = 1.0f;
        settings.language = Language.ES.code();
        return settings;
    }

    public Settings copy() {
        Settings copy = new Settings();
        copy.uiScale = uiScale;
        copy.windowWidth = windowWidth;
        copy.windowHeight = windowHeight;
        copy.language = language;
        copy.componentScales.putAll(componentScales);
        return copy;
    }

    public void copyFrom(Settings other) {
        this.uiScale = other.uiScale;
        this.windowWidth = other.windowWidth;
        this.windowHeight = other.windowHeight;
        this.language = other.language;
        this.componentScales.clear();
        this.componentScales.putAll(other.componentScales);
    }

    @JsonIgnore
    public Language getLanguageEnum() {
        return Language.fromCode(language);
    }

    public void setLanguage(String language) {
        if (language == null || language.isBlank()) {
            this.language = Language.ES.code();
        } else {
            this.language = language;
        }
    }

    @JsonIgnore
    public void setLanguageEnum(Language language) {
        setLanguage(language != null ? language.code() : null);
    }

    @JsonIgnore
    public float getComponentScale(String id) {
        return componentScales.getOrDefault(id, 1.0f);
    }

    public void setComponentScale(String id, float scale) {
        componentScales.put(id, scale);
    }

    @JsonAnySetter
    public void setDynamicProperty(String key, Object value) {
        // Allow forward compatibility by ignoring unknown fields instead of failing.
    }
}
