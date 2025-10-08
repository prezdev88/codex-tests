package cl.prezdev.envio;

import com.formdev.flatlaf.FlatDarculaLaf;

import javax.swing.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.LinkedHashMap;
import java.util.Map;

public class App {

    private static final SettingsManager SETTINGS_MANAGER = new SettingsManager();
    private static final Map<Float, JRadioButtonMenuItem> SCALE_MENU_ITEMS = new LinkedHashMap<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(App::createAndShowUI);
    }

    private static void createAndShowUI() {
        FlatDarculaLaf.setup();

        JFrame frame = new JFrame("HTTP Client Viewer");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        Settings settings = SETTINGS_MANAGER.load();
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
    }

    private static JMenuBar createMenuBar(HttpClientPanel panel, JFrame frame, Settings settings) {
        JMenuBar menuBar = new JMenuBar();

        JMenu viewMenu = new JMenu("Ver");
        menuBar.add(viewMenu);

        Map<String, Float> scales = new LinkedHashMap<>();
        scales.put("Normal", 1.0f);
        scales.put("Mediano", 1.25f);
        scales.put("Grande", 1.5f);

        SCALE_MENU_ITEMS.clear();
        ButtonGroup group = new ButtonGroup();
        for (Map.Entry<String, Float> entry : scales.entrySet()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(entry.getKey());
            float scale = entry.getValue();
            item.addActionListener(e -> {
                panel.setUiScale(scale);
                saveSettings(settings);
                updateScaleMenuSelection(scale);
            });
            group.add(item);
            SCALE_MENU_ITEMS.put(entry.getValue(), item);
            viewMenu.add(item);
        }

        JMenu settingsMenu = new JMenu("Configuración");
        JMenuItem editSettings = new JMenuItem("Editar ajustes...");
        editSettings.addActionListener(e -> {
            SettingsDialog dialog = new SettingsDialog(frame, settings, updated -> {
                panel.applySettings();
                applyWindowSize(frame, updated);
                updateScaleMenuSelection(updated.getUiScale());
                saveSettings(updated);
            });
            dialog.setVisible(true);
        });
        settingsMenu.add(editSettings);
        menuBar.add(settingsMenu);

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
}
