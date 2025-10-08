package cl.prezdev.envio;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SettingsManager {
    private static final String DIRECTORY_NAME = ".envio";
    private static final String FILE_NAME = "settings.json";

    private final ObjectMapper mapper;
    private final Path settingsPath;

    public SettingsManager() {
        this(Paths.get(System.getProperty("user.home"), DIRECTORY_NAME, FILE_NAME));
    }

    public SettingsManager(Path settingsPath) {
        this.mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        this.settingsPath = settingsPath;
    }

    public Settings load() {
        if (Files.exists(settingsPath)) {
            try {
                return mapper.readValue(settingsPath.toFile(), Settings.class);
            } catch (IOException ex) {
                System.err.println("No se pudieron cargar los ajustes: " + ex.getMessage());
            }
        }
        return Settings.defaults();
    }

    public void save(Settings settings) {
        try {
            Path directory = settingsPath.getParent();
            if (directory != null && !Files.exists(directory)) {
                Files.createDirectories(directory);
            }
            mapper.writeValue(settingsPath.toFile(), settings);
        } catch (IOException ex) {
            System.err.println("No se pudieron guardar los ajustes: " + ex.getMessage());
        }
    }

    public Path getSettingsPath() {
        return settingsPath;
    }

    public String toJson(Settings settings) {
        try {
            return mapper.writeValueAsString(settings);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    public Settings fromJson(String json) throws IOException {
        return mapper.readValue(json, Settings.class);
    }
}
