package cl.prezdev.envio;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.LinkedHashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Settings {
    private float uiScale = 1.0f;
    private int windowWidth = -1;
    private int windowHeight = -1;
    private final Map<String, Float> componentScales = new LinkedHashMap<>();

    public Settings() {
    }

    public static Settings defaults() {
        Settings settings = new Settings();
        settings.uiScale = 1.0f;
        return settings;
    }

    public Settings copy() {
        Settings copy = new Settings();
        copy.uiScale = uiScale;
        copy.windowWidth = windowWidth;
        copy.windowHeight = windowHeight;
        copy.componentScales.putAll(componentScales);
        return copy;
    }

    public void copyFrom(Settings other) {
        this.uiScale = other.uiScale;
        this.windowWidth = other.windowWidth;
        this.windowHeight = other.windowHeight;
        this.componentScales.clear();
        this.componentScales.putAll(other.componentScales);
    }

    public float getUiScale() {
        return uiScale;
    }

    public void setUiScale(float uiScale) {
        this.uiScale = uiScale;
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public void setWindowWidth(int windowWidth) {
        this.windowWidth = windowWidth;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    public void setWindowHeight(int windowHeight) {
        this.windowHeight = windowHeight;
    }

    public Map<String, Float> getComponentScales() {
        return componentScales;
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
