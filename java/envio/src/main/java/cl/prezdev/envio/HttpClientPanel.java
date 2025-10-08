package cl.prezdev.envio;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class HttpClientPanel extends JPanel {

    private static final int TEXT_AREA_ROWS = 16;
    private static final int TEXT_AREA_COLUMNS = 80;

    private static final String BASE_FONT_PROPERTY = "base-font";
    private static final float MIN_CODE_FONT_SCALE = 0.5f;
    private static final float MAX_CODE_FONT_SCALE = 3.0f;
    private static final String CODE_FONT_SCALE_PROPERTY = "code-font-scale";
    private static final String CODE_FONT_ID_PROPERTY = "code-font-id";

    private final JComboBox<HttpMethod> methodComboBox = new JComboBox<>(HttpMethod.values());
    private final JTextField urlField = new JTextField("https://jsonplaceholder.typicode.com/posts/1", 40);
    private final JTextArea requestBodyArea = createTextArea(8);
    private final JTextArea rawRequestArea = createTextArea(TEXT_AREA_ROWS / 2);
    private final JTextArea rawResponseArea = createTextArea(TEXT_AREA_ROWS / 2);
    private final JButton sendButton = new JButton("Enviar");
    private final JLabel statusLabel = new JLabel("Listo");

    private final HttpClientService httpClientService = new HttpClientService();
    private final StyleContext styleContext = new StyleContext();
    private final DefaultStyledDocument jsonDocument = new DefaultStyledDocument(styleContext);
    private final Style defaultStyle;
    private final Style keyStyle;
    private final Style stringStyle;
    private final Style numberStyle;
    private final Style literalStyle;
    private final JTextPane jsonResponsePane;
    private final JTree jsonTree;
    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final Font baseMonospacedFont = new Font(Font.MONOSPACED, Font.PLAIN, 13);
    private float uiScale = 1.0f;
    private final Map<String, JComponent> codeZoomComponents = new LinkedHashMap<>();
    private final Settings settings;
    private final Consumer<Settings> settingsChangedListener;

    public HttpClientPanel(Settings settings, Consumer<Settings> settingsChangedListener) {
        this.settings = settings;
        this.settingsChangedListener = settingsChangedListener;
        defaultStyle = styleContext.getStyle(StyleContext.DEFAULT_STYLE);
        keyStyle = styleContext.addStyle("json-key", defaultStyle);
        StyleConstants.setForeground(keyStyle, new Color(204, 120, 50));

        stringStyle = styleContext.addStyle("json-string", defaultStyle);
        StyleConstants.setForeground(stringStyle, new Color(106, 135, 89));

        numberStyle = styleContext.addStyle("json-number", defaultStyle);
        StyleConstants.setForeground(numberStyle, new Color(104, 151, 187));

        literalStyle = styleContext.addStyle("json-literal", defaultStyle);
        StyleConstants.setForeground(literalStyle, new Color(152, 118, 170));

        jsonResponsePane = createJsonTextPane();
        jsonTree = createJsonTree();

        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        add(createInputPanel(), BorderLayout.NORTH);
        add(createResultPanel(), BorderLayout.CENTER);
        add(createStatusPanel(), BorderLayout.SOUTH);

        updateJsonDisplay("");

        sendButton.addActionListener(event -> executeRequest());
        methodComboBox.addItemListener(event -> {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                toggleRequestBody();
            }
        });
        toggleRequestBody();

        registerBaseFonts(this);
        installCodeFontZoom("requestBody", requestBodyArea);
        installCodeFontZoom("rawRequest", rawRequestArea);
        installCodeFontZoom("rawResponse", rawResponseArea);
        installCodeFontZoom("jsonResponse", jsonResponsePane);
        installCodeFontZoom("jsonTree", jsonTree);
        applySettings();
    }

    public void setUiScale(float scale) {
        setUiScale(scale, true);
    }

    private void setUiScale(float scale, boolean updateSettings) {
        if (scale <= 0f) {
            return;
        }
        float clampedScale = Math.max(0.5f, Math.min(2.5f, scale));
        if (Math.abs(clampedScale - uiScale) < 0.001f) {
            return;
        }
        uiScale = clampedScale;
        applyScaleToComponent(this, uiScale);
        updateCodeFonts();
        revalidate();
        repaint();
        if (updateSettings) {
            settings.setUiScale(uiScale);
            persistSettings();
        }
    }

    private void registerBaseFonts(Component component) {
        if (component instanceof JComponent jComponent) {
            if (jComponent.getClientProperty(BASE_FONT_PROPERTY) == null) {
                Font font = component.getFont();
                if (font != null) {
                    jComponent.putClientProperty(BASE_FONT_PROPERTY, font);
                }
            }
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                registerBaseFonts(child);
            }
        }
    }

    private void applyScaleToComponent(Component component, float scale) {
        if (component instanceof JComponent jComponent) {
            Font baseFont = (Font) jComponent.getClientProperty(BASE_FONT_PROPERTY);
            if (baseFont == null) {
                baseFont = component.getFont();
                if (baseFont != null) {
                    jComponent.putClientProperty(BASE_FONT_PROPERTY, baseFont);
                }
            }
            if (baseFont != null) {
                component.setFont(baseFont.deriveFont(baseFont.getSize2D() * scale));
            }
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                applyScaleToComponent(child, scale);
            }
        }
    }

    private void installCodeFontZoom(String id, JComponent component) {
        codeZoomComponents.put(id, component);
        component.putClientProperty(CODE_FONT_ID_PROPERTY, id);
        component.putClientProperty(CODE_FONT_SCALE_PROPERTY, settings.getComponentScale(id));
        component.addMouseWheelListener(event -> {
            if (event.isControlDown()) {
                event.consume();
                int rotation = event.getWheelRotation();
                if (rotation < 0) {
                    adjustCodeFontScale(component, 0.1f);
                } else if (rotation > 0) {
                    adjustCodeFontScale(component, -0.1f);
                }
            }
        });
    }

    private void adjustCodeFontScale(JComponent component, float delta) {
        float currentScale = getCodeFontScale(component);
        float newScale = Math.max(MIN_CODE_FONT_SCALE, Math.min(MAX_CODE_FONT_SCALE, currentScale + delta));
        if (Math.abs(newScale - currentScale) > 0.001f) {
            component.putClientProperty(CODE_FONT_SCALE_PROPERTY, newScale);
            updateCodeFontForComponent(component);
            Object idValue = component.getClientProperty(CODE_FONT_ID_PROPERTY);
            if (idValue instanceof String id) {
                settings.setComponentScale(id, newScale);
                persistSettings();
            }
        }
    }

    private float getCodeFontScale(JComponent component) {
        Object value = component.getClientProperty(CODE_FONT_SCALE_PROPERTY);
        if (value instanceof Number number) {
            return number.floatValue();
        }
        return 1.0f;
    }

    private void updateCodeFonts() {
        for (JComponent component : codeZoomComponents.values()) {
            updateCodeFontForComponent(component);
        }
    }

    private void updateCodeFontForComponent(JComponent component) {
        float componentScale = getCodeFontScale(component);
        float scaledSize = baseMonospacedFont.getSize2D() * uiScale * componentScale;
        Font codeFont = baseMonospacedFont.deriveFont(scaledSize);
        component.setFont(codeFont);
        if (component == jsonResponsePane) {
            updateJsonStyles(codeFont);
        }
        if (component instanceof JTree tree) {
            int rowHeight = Math.max(16, Math.round(codeFont.getSize2D() * 1.4f));
            tree.setRowHeight(rowHeight);
        }
    }

    private void updateJsonStyles(Font font) {
        applyFontToStyle(defaultStyle, font);
        applyFontToStyle(keyStyle, font);
        applyFontToStyle(stringStyle, font);
        applyFontToStyle(numberStyle, font);
        applyFontToStyle(literalStyle, font);
    }

    private void applyFontToStyle(Style style, Font font) {
        StyleConstants.setFontFamily(style, font.getFamily());
        StyleConstants.setFontSize(style, Math.round(font.getSize2D()));
    }

    public void applySettings() {
        setUiScale(settings.getUiScale(), false);
        for (Map.Entry<String, JComponent> entry : codeZoomComponents.entrySet()) {
            String id = entry.getKey();
            JComponent component = entry.getValue();
            component.putClientProperty(CODE_FONT_SCALE_PROPERTY, settings.getComponentScale(id));
        }
        updateCodeFonts();
    }

    private void persistSettings() {
        if (settingsChangedListener != null) {
            settingsChangedListener.accept(settings);
        }
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
        rawRequestArea.setEditable(false);
        rawResponseArea.setEditable(false);
        rawRequestArea.setLineWrap(false);
        rawResponseArea.setLineWrap(false);

        JSplitPane jsonSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                createTitledScrollPane("Árbol JSON", jsonTree),
                createTitledScrollPane("Respuesta JSON formateada", jsonResponsePane));
        jsonSplit.setResizeWeight(0.35);
        jsonSplit.setOneTouchExpandable(true);

        JSplitPane bottomSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                createTitledScrollPane("Request HTTP crudo", rawRequestArea),
                createTitledScrollPane("Response HTTP crudo", rawResponseArea));
        bottomSplit.setResizeWeight(0.5);
        bottomSplit.setOneTouchExpandable(true);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, jsonSplit, bottomSplit);
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
                    updateJsonDisplay(result.formattedBody());
                    rawRequestArea.setText(result.rawRequest());
                    rawResponseArea.setText(result.rawResponse());
                    boolean hasError = result.hasError();
                    updateStatus(hasError ? result.errorMessage() : "Operación completada", hasError);
                } catch (Exception ex) {
                    updateJsonDisplay("");
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

    private JScrollPane createTitledScrollPane(String title, JComponent component) {
        JScrollPane scrollPane = new JScrollPane(component);
        scrollPane.setBorder(BorderFactory.createTitledBorder(title));
        return scrollPane;
    }

    private JTextArea createTextArea(int rows) {
        JTextArea area = new JTextArea(rows, TEXT_AREA_COLUMNS);
        area.setFont(baseMonospacedFont);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        return area;
    }

    private JTextPane createJsonTextPane() {
        JTextPane pane = new JTextPane(jsonDocument);
        pane.setEditable(false);
        pane.setFont(baseMonospacedFont);
        pane.setMargin(new Insets(8, 8, 8, 8));
        return pane;
    }

    private JTree createJsonTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Sin datos");
        JTree tree = new JTree(new DefaultTreeModel(root));
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.setFont(baseMonospacedFont);
        return tree;
    }

    private void updateJsonDisplay(String formattedBody) {
        applyJsonHighlight(formattedBody);
        updateJsonTree(formattedBody);
    }

    private void applyJsonHighlight(String text) {
        if (text == null) {
            text = "";
        }

        try {
            jsonDocument.remove(0, jsonDocument.getLength());
            jsonDocument.insertString(0, text, defaultStyle);
        } catch (BadLocationException ex) {
            return;
        }

        int length = text.length();
        int index = 0;
        while (index < length) {
            char current = text.charAt(index);
            if (current == '"') {
                int start = index;
                index++;
                boolean escaped = false;
                while (index < length) {
                    char value = text.charAt(index);
                    if (value == '\\' && !escaped) {
                        escaped = true;
                    } else {
                        if (value == '"' && !escaped) {
                            index++;
                            break;
                        }
                        escaped = false;
                    }
                    index++;
                }
                int end = Math.min(index, length);
                int lookAhead = end;
                while (lookAhead < length && Character.isWhitespace(text.charAt(lookAhead))) {
                    lookAhead++;
                }
                boolean isKey = lookAhead < length && text.charAt(lookAhead) == ':';
                jsonDocument.setCharacterAttributes(start, end - start, isKey ? keyStyle : stringStyle, true);
                continue;
            }

            if (Character.isDigit(current) || (current == '-' && index + 1 < length && Character.isDigit(text.charAt(index + 1)))) {
                int start = index;
                index++;
                while (index < length) {
                    char value = text.charAt(index);
                    if (Character.isDigit(value) || value == '.' || value == 'e' || value == 'E' || value == '+' || value == '-') {
                        index++;
                    } else {
                        break;
                    }
                }
                jsonDocument.setCharacterAttributes(start, index - start, numberStyle, true);
                continue;
            }

            if (Character.isLetter(current)) {
                int start = index;
                while (index < length && Character.isLetter(text.charAt(index))) {
                    index++;
                }
                String word = text.substring(start, index);
                if ("true".equals(word) || "false".equals(word) || "null".equals(word)) {
                    jsonDocument.setCharacterAttributes(start, index - start, literalStyle, true);
                }
                continue;
            }

            index++;
        }

        jsonResponsePane.setCaretPosition(0);
    }

    private void updateJsonTree(String formattedBody) {
        DefaultTreeModel model = (DefaultTreeModel) jsonTree.getModel();
        if (formattedBody == null || formattedBody.isBlank()) {
            model.setRoot(new DefaultMutableTreeNode("Sin datos"));
            model.reload();
            expandAllRows(jsonTree);
            return;
        }

        try {
            JsonNode rootNode = jsonMapper.readTree(formattedBody);
            DefaultMutableTreeNode treeRoot = buildTreeNode(null, rootNode);
            model.setRoot(treeRoot);
        } catch (JsonProcessingException ex) {
            model.setRoot(new DefaultMutableTreeNode("No es JSON válido"));
        }
        model.reload();
        expandAllRows(jsonTree);
    }

    private DefaultMutableTreeNode buildTreeNode(String name, JsonNode node) {
        if (node.isObject()) {
            String label = name == null ? "Objeto" : name;
            DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(label);
            node.fields().forEachRemaining(entry -> treeNode.add(buildTreeNode(entry.getKey(), entry.getValue())));
            return treeNode;
        }

        if (node.isArray()) {
            String label = name == null ? "Arreglo" : name;
            DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(label);
            for (int i = 0; i < node.size(); i++) {
                treeNode.add(buildTreeNode("[" + i + "]", node.get(i)));
            }
            return treeNode;
        }

        String value = node.isTextual() ? '"' + node.asText() + '"' : node.toString();
        String label = name == null ? value : name + ": " + value;
        return new DefaultMutableTreeNode(label);
    }

    private void expandAllRows(JTree tree) {
        int row = 0;
        while (row < tree.getRowCount()) {
            tree.expandRow(row);
            row++;
        }
    }

    private void updateStatus(String message, boolean error) {
        statusLabel.setText(message);
        statusLabel.setForeground(error ? new Color(255, 105, 97) : UIManager.getColor("Label.foreground"));
    }
}
