package cl.prezdev.envio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class SettingsDialog extends JDialog {
    private final Settings settings;
    private final Settings workingCopy;
    private final Consumer<Settings> onApply;
    private final Map<String, JSpinner> componentSpinners = new LinkedHashMap<>();
    private final JSpinner uiScaleSpinner;
    private final JSpinner widthSpinner;
    private final JSpinner heightSpinner;
    private final JTextArea jsonEditor;
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public SettingsDialog(Frame owner, Settings settings, Consumer<Settings> onApply) {
        super(owner, "Ajustes", true);
        this.settings = settings;
        this.workingCopy = settings.copy();
        this.onApply = onApply;

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(12, 12));

        this.uiScaleSpinner = createUiScaleSpinner();
        this.widthSpinner = createWidthSpinner();
        this.heightSpinner = createHeightSpinner();
        this.jsonEditor = createJsonEditor();

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Gráfico", createVisualPanel());
        tabs.addTab("JSON", createJsonPanel());
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 1) {
                refreshJsonEditor();
            }
        });
        add(tabs, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton("Cancelar");
        cancelButton.addActionListener(e -> dispose());
        JButton saveButton = new JButton("Guardar");
        saveButton.addActionListener(e -> onSave());
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                refreshJsonEditor();
            }
        });
    }

    private JPanel createVisualPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;

        panel.add(new JLabel("Escala de interfaz"), gbc);
        gbc.gridx = 1;
        panel.add(uiScaleSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Ancho de ventana"), gbc);
        gbc.gridx = 1;
        panel.add(widthSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Alto de ventana"), gbc);
        gbc.gridx = 1;
        panel.add(heightSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        panel.add(new JLabel("Zoom por componente"), gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;

        Map<String, String> labels = Map.of(
                "requestBody", "Cuerpo de solicitud",
                "rawRequest", "Request crudo",
                "rawResponse", "Response crudo",
                "jsonResponse", "JSON formateado",
                "jsonTree", "Árbol JSON"
        );

        for (Map.Entry<String, String> entry : labels.entrySet()) {
            gbc.gridx = 0;
            panel.add(new JLabel(entry.getValue()), gbc);
            gbc.gridx = 1;
            double scaleValue = workingCopy.getComponentScale(entry.getKey());
            JSpinner spinner = new JSpinner(new SpinnerNumberModel(scaleValue, 0.5, 3.0, 0.05));
            workingCopy.setComponentScale(entry.getKey(), ((Double) spinner.getValue()).floatValue());
            spinner.addChangeListener(spinnerListener(value -> workingCopy.setComponentScale(entry.getKey(), value.floatValue())));
            componentSpinners.put(entry.getKey(), spinner);
            panel.add(spinner, gbc);
            gbc.gridy++;
        }

        return panel;
    }

    private JPanel createJsonPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(jsonEditor), BorderLayout.CENTER);
        JLabel infoLabel = new JLabel("Modifique el JSON y presione Guardar para aplicar los cambios.");
        panel.add(infoLabel, BorderLayout.SOUTH);
        return panel;
    }

    private JSpinner createUiScaleSpinner() {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel((double) workingCopy.getUiScale(), 0.5, 2.5, 0.05));
        workingCopy.setUiScale(((Double) spinner.getValue()).floatValue());
        spinner.addChangeListener(spinnerListener(value -> workingCopy.setUiScale(value.floatValue())));
        return spinner;
    }

    private JSpinner createWidthSpinner() {
        int widthValue = Math.max(workingCopy.getWindowWidth(), 800);
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(widthValue, 400, 4096, 10));
        workingCopy.setWindowWidth((Integer) spinner.getValue());
        spinner.addChangeListener(spinnerListener(value -> workingCopy.setWindowWidth(value.intValue())));
        return spinner;
    }

    private JSpinner createHeightSpinner() {
        int heightValue = Math.max(workingCopy.getWindowHeight(), 600);
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(heightValue, 300, 2160, 10));
        workingCopy.setWindowHeight((Integer) spinner.getValue());
        spinner.addChangeListener(spinnerListener(value -> workingCopy.setWindowHeight(value.intValue())));
        return spinner;
    }

    private JTextArea createJsonEditor() {
        JTextArea editor = new JTextArea(20, 60);
        editor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        return editor;
    }

    private ChangeListener spinnerListener(Consumer<Number> consumer) {
        return new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                Object value = ((JSpinner) e.getSource()).getValue();
                if (value instanceof Number number) {
                    consumer.accept(number);
                    refreshJsonEditor();
                }
            }
        };
    }

    private void refreshJsonEditor() {
        try {
            jsonEditor.setText(mapper.writeValueAsString(workingCopy));
        } catch (IOException e) {
            jsonEditor.setText("{}");
        }
    }

    private void onSave() {
        try {
            Settings parsed = mapper.readValue(jsonEditor.getText(), Settings.class);
            settings.copyFrom(parsed);
            if (onApply != null) {
                onApply.accept(settings);
            }
            dispose();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo interpretar el JSON ingresado: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
