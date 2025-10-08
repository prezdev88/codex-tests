package cl.prezdev.envio;

import com.formdev.flatlaf.FlatDarculaLaf;

import javax.swing.*;
import java.util.LinkedHashMap;
import java.util.Map;
public class App {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(App::createAndShowUI);
    }

    private static void createAndShowUI() {
        FlatDarculaLaf.setup();

        JFrame frame = new JFrame("HTTP Client Viewer");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        HttpClientPanel panel = new HttpClientPanel();
        frame.setContentPane(panel);
        frame.setJMenuBar(createMenuBar(panel));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static JMenuBar createMenuBar(HttpClientPanel panel) {
        JMenuBar menuBar = new JMenuBar();

        JMenu viewMenu = new JMenu("Ver");
        menuBar.add(viewMenu);

        Map<String, Float> scales = new LinkedHashMap<>();
        scales.put("Normal", 1.0f);
        scales.put("Mediano", 1.25f);
        scales.put("Grande", 1.5f);

        ButtonGroup group = new ButtonGroup();
        boolean first = true;
        for (Map.Entry<String, Float> entry : scales.entrySet()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(entry.getKey());
            if (first) {
                item.setSelected(true);
                first = false;
            }
            item.addActionListener(e -> panel.setUiScale(entry.getValue()));
            group.add(item);
            viewMenu.add(item);
        }

        return menuBar;
    }
}
