package cl.prezdev.envio;

import com.formdev.flatlaf.FlatDarculaLaf;

import javax.swing.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class App {

    private static final SettingsManager SETTINGS_MANAGER = new SettingsManager();
    private static final Map<Float, JRadioButtonMenuItem> SCALE_MENU_ITEMS = new LinkedHashMap<>();
    private static final Map<Language, JRadioButtonMenuItem> LANGUAGE_MENU_ITEMS = new LinkedHashMap<>();
    private static Language currentLanguage = Language.ES;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(App::createAndShowUI);
    }

    private static void createAndShowUI() {
        FlatDarculaLaf.setup();

        Settings settings = SETTINGS_MANAGER.load();
        currentLanguage = settings.getLanguageEnum();
        JFrame frame = new JFrame(I18n.app(currentLanguage).windowTitle());
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        HttpClientPanel panel = new HttpClientPanel(settings, App::saveSettings);
        frame.setContentPane(panel);
        frame.setJMenuBar(createMenuBar(panel, frame, settings));
        applyWindowSize(frame, settings);
        panel.applySettings();
        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (!frame.isShowing()) {
                    return;
                }
                settings.setWindowWidth(frame.getWidth());
                settings.setWindowHeight(frame.getHeight());
                saveSettings(settings);
            }
        });
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        updateScaleMenuSelection(settings.getUiScale());
        updateLanguageMenuSelection(currentLanguage);
    }

    private static JMenuBar createMenuBar(HttpClientPanel panel, JFrame frame, Settings settings) {
        JMenuBar menuBar = new JMenuBar();
        I18n.AppTexts texts = I18n.app(currentLanguage);

        JMenu viewMenu = new JMenu(texts.viewMenu());
        menuBar.add(viewMenu);

        Map<String, Float> scales = new LinkedHashMap<>();
        scales.put(texts.viewScaleNormal(), 1.0f);
        scales.put(texts.viewScaleMedium(), 1.25f);
        scales.put(texts.viewScaleLarge(), 1.5f);

        SCALE_MENU_ITEMS.clear();
        ButtonGroup scaleGroup = new ButtonGroup();
        for (Map.Entry<String, Float> entry : scales.entrySet()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(entry.getKey());
            float scale = entry.getValue();
            item.addActionListener(e -> {
                panel.setUiScale(scale);
                saveSettings(settings);
                updateScaleMenuSelection(scale);
            });
            scaleGroup.add(item);
            SCALE_MENU_ITEMS.put(entry.getValue(), item);
            viewMenu.add(item);
        }

        JMenu settingsMenu = new JMenu(texts.settingsMenu());
        JMenuItem editSettings = new JMenuItem(texts.editSettings());
        editSettings.addActionListener(e -> {
            SettingsDialog dialog = new SettingsDialog(frame, settings, SETTINGS_MANAGER.getSettingsPath(), updated -> {
                panel.applySettings();
                applyWindowSize(frame, updated);
                updateScaleMenuSelection(updated.getUiScale());
                saveSettings(updated);
            });
            dialog.setVisible(true);
        });
        settingsMenu.add(editSettings);
        menuBar.add(settingsMenu);

        JMenu languageMenu = new JMenu(texts.languageMenu());
        ButtonGroup languageGroup = new ButtonGroup();
        LANGUAGE_MENU_ITEMS.clear();
        for (Language language : Language.values()) {
            String label = language == Language.ES ? texts.languageSpanish() : texts.languageEnglish();
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(label);
            item.addActionListener(e -> changeLanguage(language, panel, frame, settings));
            languageGroup.add(item);
            LANGUAGE_MENU_ITEMS.put(language, item);
            languageMenu.add(item);
        }
        menuBar.add(languageMenu);

        JMenu samplesMenu = new JMenu(texts.samplesMenu());
        for (ApiSample sample : sampleApis()) {
            JMenuItem item = new JMenuItem(sample.name());
            if (sample.description() != null && !sample.description().isBlank()) {
                item.setToolTipText(sample.description());
            }
            item.addActionListener(e -> panel.applySample(
                    sample.method(),
                    sample.url(),
                    sample.body(),
                    String.format(texts.sampleLoadedPattern(), sample.name())
            ));
            samplesMenu.add(item);
        }
        menuBar.add(samplesMenu);

        return menuBar;
    }

    private static void updateScaleMenuSelection(float scale) {
        SCALE_MENU_ITEMS.forEach((value, item) -> {
            if (Math.abs(value - scale) < 0.001f) {
                item.setSelected(true);
            } else {
                item.setSelected(false);
            }
        });
    }

    private static void updateLanguageMenuSelection(Language language) {
        LANGUAGE_MENU_ITEMS.forEach((lang, item) -> item.setSelected(lang == language));
    }

    private static void applyWindowSize(JFrame frame, Settings settings) {
        int width = settings.getWindowWidth();
        int height = settings.getWindowHeight();
        if (width > 0 && height > 0) {
            frame.setSize(width, height);
        } else {
            frame.pack();
        }
    }

    private static void saveSettings(Settings settings) {
        SETTINGS_MANAGER.save(settings);
    }

    private static void changeLanguage(Language language, HttpClientPanel panel, JFrame frame, Settings settings) {
        if (language == null || language == currentLanguage) {
            return;
        }
        currentLanguage = language;
        panel.setLanguage(language);
        frame.setTitle(I18n.app(language).windowTitle());
        frame.setJMenuBar(createMenuBar(panel, frame, settings));
        frame.revalidate();
        frame.repaint();
        updateScaleMenuSelection(settings.getUiScale());
        updateLanguageMenuSelection(language);
    }

    private static List<ApiSample> sampleApis() {
        return List.of(
                new ApiSample(
                        "JSONPlaceholder · Post 1",
                        "Obtiene un post de ejemplo desde jsonplaceholder.typicode.com",
                        HttpMethod.GET,
                        "https://jsonplaceholder.typicode.com/posts/1",
                        null
                ),
                new ApiSample(
                        "JSONPlaceholder · Crear Post",
                        "Envía un POST de ejemplo a jsonplaceholder.typicode.com",
                        HttpMethod.POST,
                        "https://jsonplaceholder.typicode.com/posts",
                        "{\n  \"title\": \"foo\",\n  \"body\": \"bar\",\n  \"userId\": 1\n}"
                ),
                new ApiSample(
                        "ReqRes · Usuario",
                        "Obtiene un usuario de reqres.in",
                        HttpMethod.GET,
                        "https://reqres.in/api/users/2",
                        null
                ),
                new ApiSample(
                        "Cat Facts",
                        "Devuelve un dato curioso sobre gatos",
                        HttpMethod.GET,
                        "https://catfact.ninja/fact",
                        null
                ),
                new ApiSample(
                        "Advice Slip",
                        "Recibe un consejo aleatorio",
                        HttpMethod.GET,
                        "https://api.adviceslip.com/advice",
                        null
                )
        );
    }

    private record ApiSample(String name, String description, HttpMethod method, String url, String body) {
    }
}
