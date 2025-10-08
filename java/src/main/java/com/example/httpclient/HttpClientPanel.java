package com.example.httpclient;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ItemEvent;

public class HttpClientPanel extends JPanel {

    private static final int TEXT_AREA_ROWS = 16;
    private static final int TEXT_AREA_COLUMNS = 80;

    private final JComboBox<HttpMethod> methodComboBox = new JComboBox<>(HttpMethod.values());
    private final JTextField urlField = new JTextField("https://jsonplaceholder.typicode.com/posts/1", 40);
    private final JTextArea requestBodyArea = createTextArea(8);
    private final JTextArea jsonResponseArea = createTextArea(TEXT_AREA_ROWS);
    private final JTextArea rawRequestArea = createTextArea(TEXT_AREA_ROWS / 2);
    private final JTextArea rawResponseArea = createTextArea(TEXT_AREA_ROWS / 2);
    private final JButton sendButton = new JButton("Enviar");
    private final JLabel statusLabel = new JLabel("Listo");

    private final HttpClientService httpClientService = new HttpClientService();

    public HttpClientPanel() {
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        add(createInputPanel(), BorderLayout.NORTH);
        add(createResultPanel(), BorderLayout.CENTER);
        add(createStatusPanel(), BorderLayout.SOUTH);

        sendButton.addActionListener(event -> executeRequest());
        methodComboBox.addItemListener(event -> {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                toggleRequestBody();
            }
        });
        toggleRequestBody();
    }

    private JPanel createInputPanel() {
        JPanel container = new JPanel();
        container.setLayout(new BorderLayout(8, 8));

        JPanel methodUrlPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 8);
        gbc.gridx = 0;
        methodUrlPanel.add(new JLabel("Método"), gbc);

        gbc.gridx = 1;
        methodComboBox.setPreferredSize(new Dimension(120, methodComboBox.getPreferredSize().height));
        methodUrlPanel.add(methodComboBox, gbc);

        gbc.gridx = 2;
        methodUrlPanel.add(new JLabel("URL"), gbc);

        gbc.gridx = 3;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        methodUrlPanel.add(urlField, gbc);

        gbc.gridx = 4;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        methodUrlPanel.add(sendButton, gbc);

        container.add(methodUrlPanel, BorderLayout.NORTH);

        JPanel bodyPanel = new JPanel(new BorderLayout(4, 4));
        bodyPanel.add(new JLabel("Cuerpo (JSON opcional)"), BorderLayout.NORTH);
        bodyPanel.add(new JScrollPane(requestBodyArea), BorderLayout.CENTER);
        container.add(bodyPanel, BorderLayout.CENTER);

        return container;
    }

    private Component createResultPanel() {
        jsonResponseArea.setEditable(false);
        rawRequestArea.setEditable(false);
        rawResponseArea.setEditable(false);
        rawRequestArea.setLineWrap(false);
        rawResponseArea.setLineWrap(false);

        JSplitPane bottomSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                createTitledScrollPane("Request HTTP crudo", rawRequestArea),
                createTitledScrollPane("Response HTTP crudo", rawResponseArea));
        bottomSplit.setResizeWeight(0.5);
        bottomSplit.setOneTouchExpandable(true);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                createTitledScrollPane("Respuesta JSON formateada", jsonResponseArea), bottomSplit);
        mainSplit.setResizeWeight(0.6);
        mainSplit.setOneTouchExpandable(true);
        mainSplit.setContinuousLayout(true);

        return mainSplit;
    }

    private JPanel createStatusPanel() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
        statusPanel.add(statusLabel, BorderLayout.WEST);
        return statusPanel;
    }

    private void executeRequest() {
        String url = urlField.getText().trim();
        HttpMethod method = (HttpMethod) methodComboBox.getSelectedItem();
        String body = requestBodyArea.getText();

        if (url.isEmpty()) {
            updateStatus("La URL es obligatoria", true);
            return;
        }

        sendButton.setEnabled(false);
        updateStatus("Llamando al endpoint...", false);

        SwingWorker<HttpInteractionResult, Void> worker = new SwingWorker<>() {
            @Override
            protected HttpInteractionResult doInBackground() {
                return httpClientService.execute(method, url, body);
            }

            @Override
            protected void done() {
                try {
                    HttpInteractionResult result = get();
                    jsonResponseArea.setText(result.formattedBody());
                    rawRequestArea.setText(result.rawRequest());
                    rawResponseArea.setText(result.rawResponse());
                    boolean hasError = result.hasError();
                    updateStatus(hasError ? result.errorMessage() : "Operación completada", hasError);
                } catch (Exception ex) {
                    jsonResponseArea.setText("");
                    rawRequestArea.setText("");
                    rawResponseArea.setText("");
                    updateStatus("Error ejecutando la petición: " + ex.getMessage(), true);
                } finally {
                    sendButton.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    private void toggleRequestBody() {
        HttpMethod selected = (HttpMethod) methodComboBox.getSelectedItem();
        boolean bodyEnabled = selected != null && selected.allowsBody();
        requestBodyArea.setEnabled(bodyEnabled);
        requestBodyArea.setEditable(bodyEnabled);
        requestBodyArea.setBackground(bodyEnabled ? UIManager.getColor("TextArea.background") :
                UIManager.getColor("Panel.background"));
        if (!bodyEnabled) {
            requestBodyArea.setText("");
        }
    }

    private JScrollPane createTitledScrollPane(String title, JTextArea textArea) {
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder(title));
        return scrollPane;
    }

    private JTextArea createTextArea(int rows) {
        JTextArea area = new JTextArea(rows, TEXT_AREA_COLUMNS);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        return area;
    }

    private void updateStatus(String message, boolean error) {
        statusLabel.setText(message);
        statusLabel.setForeground(error ? new Color(255, 105, 97) : UIManager.getColor("Label.foreground"));
    }
}
