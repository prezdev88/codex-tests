import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.UIDefaults;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.DimensionUIResource;
import javax.swing.plaf.InsetsUIResource;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RunGui {

    private static final Path ROOT = Paths.get("").toAbsolutePath();
    private static final String PROJECT_FILE_NAME = "project.json";
    private static final int MAX_SCAN_DEPTH = 6;

    private static final Map<Object, Font> BASE_DEFAULT_FONTS = new LinkedHashMap<>();
    private static final Map<Object, Insets> BASE_DEFAULT_INSETS = new LinkedHashMap<>();
    private static final Map<Object, Dimension> BASE_DEFAULT_DIMENSIONS = new LinkedHashMap<>();
    private static boolean defaultsCaptured = false;

    private static final String BASE_FONT_PROPERTY = "codex.baseFont";
    private static final String BASE_BORDER_INSETS_PROPERTY = "codex.baseBorderInsets";
    private static final String BASE_MARGIN_PROPERTY = "codex.baseMargin";
    private static final String BASE_DIVIDER_PROPERTY = "codex.baseDivider";

    private final DefaultListModel<Project> projectModel = new DefaultListModel<>();
    private final JList<Project> projectList = new JList<>(projectModel);
    private final JTextArea detailsArea = new JTextArea();
    private final JTextArea outputArea = new JTextArea();
    private final JButton runButton = new JButton("Run");
    private final JButton refreshButton = new JButton("Refresh");
    private final JLabel statusLabel = new JLabel("Ready");

    private List<Project> projects = List.of();

    public static void main(String[] args) {
        System.setProperty("swing.aatext", "true");
        System.setProperty("awt.useSystemAAFontSettings", "on");
        SwingUtilities.invokeLater(() -> {
            setupDarkTheme();
            applyDesktopHints();
            captureBaseDefaults();
            new RunGui().start();
        });
    }

    private void start() {
        projects = discoverProjects();
        buildUi();
        populateProjects();
    }

    private static void setupDarkTheme() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ignored) {
        }
        Color background = new Color(30, 32, 36);
        Color foreground = new Color(220, 223, 228);
        Color accent = new Color(70, 130, 180);
        UIManager.put("control", background);
        UIManager.put("info", background);
        UIManager.put("nimbusBase", new Color(45, 49, 56));
        UIManager.put("nimbusBlueGrey", new Color(70, 74, 82));
        UIManager.put("nimbusLightBackground", new Color(38, 40, 45));
        UIManager.put("text", foreground);
        UIManager.put("MenuItem[Enabled].textForeground", foreground);
        UIManager.put("List[Selected].textForeground", foreground);
        UIManager.put("List.background", new Color(35, 37, 42));
        UIManager.put("List.foreground", foreground);
        UIManager.put("TextArea.background", new Color(28, 30, 35));
        UIManager.put("TextArea.foreground", foreground);
        UIManager.put("TextArea.caretForeground", foreground);
        UIManager.put("Button.background", new Color(45, 48, 54));
        UIManager.put("Button.foreground", foreground);
        UIManager.put("Button.select", accent);
        UIManager.put("Label.foreground", foreground);
    }

    private JFrame frame;

    private void buildUi() {
        frame = new JFrame("Codex Project Launcher");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(960, 600));

        frame.setJMenuBar(createMenuBar());

        projectList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        projectList.setCellRenderer(new ProjectCellRenderer());
        projectList.addListSelectionListener(new ProjectSelectionListener());

        detailsArea.setEditable(false);
        detailsArea.setWrapStyleWord(true);
        detailsArea.setLineWrap(true);
        detailsArea.setOpaque(true);
        detailsArea.setBackground(new Color(28, 30, 35));
        detailsArea.setForeground(new Color(220, 223, 228));
        detailsArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        detailsArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        outputArea.setEditable(false);
        outputArea.setWrapStyleWord(true);
        outputArea.setLineWrap(true);
        outputArea.setOpaque(true);
        outputArea.setBackground(new Color(24, 26, 30));
        outputArea.setForeground(new Color(200, 204, 209));
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        outputArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane listScroll = new JScrollPane(projectList);
        JScrollPane detailsScroll = new JScrollPane(detailsArea);
        JScrollPane outputScroll = new JScrollPane(outputArea);
        detailsScroll.setBorder(BorderFactory.createTitledBorder("Details"));
        outputScroll.setBorder(BorderFactory.createTitledBorder("Output"));

        JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, detailsScroll, outputScroll);
        rightSplit.setResizeWeight(0.4);
        rightSplit.setBorder(null);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScroll, rightSplit);
        mainSplit.setResizeWeight(0.33);
        mainSplit.setBorder(null);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        topPanel.setOpaque(false);
        runButton.setEnabled(false);
        runButton.addActionListener(e -> runSelectedProject());
        refreshButton.addActionListener(e -> refreshProjects());
        topPanel.add(runButton);
        topPanel.add(refreshButton);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        statusLabel.setForeground(new Color(160, 165, 170));
        bottomPanel.add(statusLabel, BorderLayout.WEST);

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(mainSplit, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        registerComponentTree(frame.getJMenuBar());
        registerComponentTree(frame.getContentPane());
        applyScaleToComponentTree(frame.getJMenuBar(), currentScale);
        applyScaleToComponentTree(frame.getContentPane(), currentScale);
        registerAccelerators(frame);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        refreshScaledUI();
    }

    private static void applyDesktopHints() {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Object desktopHints = toolkit.getDesktopProperty("awt.font.desktophints");
        if (desktopHints instanceof Map<?, ?> hints) {
            hints.forEach((key, value) -> UIManager.put(key, value));
        }
    }

    private void populateProjects() {
        projectModel.clear();
        for (Project project : projects) {
            projectModel.addElement(project);
        }

        if (!projects.isEmpty()) {
            projectList.setSelectedIndex(0);
            runButton.setEnabled(true);
            statusLabel.setText("Select a project and press Run.");
        } else {
            detailsArea.setText("No projects found. Add project.json files to project directories.");
            runButton.setEnabled(false);
            statusLabel.setText("No projects available.");
        }
    }

    private void refreshProjects() {
        statusLabel.setText("Refreshing projects...");
        projects = discoverProjects();
        populateProjects();
        outputArea.setText("");
        statusLabel.setText("Projects refreshed.");
    }

    private void runSelectedProject() {
        Project selected = projectList.getSelectedValue();
        if (selected == null) {
            return;
        }
        runButton.setEnabled(false);
        statusLabel.setText("Running " + selected.displayName() + "...");
        outputArea.setText("");

        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                executeProject(selected);
                return null;
            }

            @Override
            protected void done() {
                runButton.setEnabled(projectModel.size() > 0);
                statusLabel.setText("Ready");
            }
        };
        worker.execute();
    }

    private void executeProject(Project project) {
        switch (project.type()) {
            case BROWSER -> openInBrowser(project);
            case SHELL -> runShellCommand(project);
        }
    }

    private void openInBrowser(Project project) {
        String command = project.command();
        if (command == null || command.isBlank()) {
            appendOutput("No path configured for " + project.displayName());
            return;
        }
        try {
            java.net.URI uri;
            if (command.startsWith("http://") || command.startsWith("https://") || command.startsWith("file:")) {
                uri = java.net.URI.create(command);
            } else {
                Path resolved = ROOT.resolve(command).normalize();
                if (!Files.exists(resolved)) {
                    appendOutput("File not found: " + resolved);
                    return;
                }
                uri = resolved.toUri();
            }
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                appendOutput("Desktop browsing not supported. Open manually: " + uri);
                return;
            }
            appendOutput("Opening " + project.displayName() + " in browser: " + uri);
            Desktop.getDesktop().browse(uri);
        } catch (IOException | IllegalArgumentException ex) {
            appendOutput("Failed to open browser: " + ex.getMessage());
        }
    }

    private void runShellCommand(Project project) {
        String command = project.command();
        if (command == null || command.isBlank()) {
            appendOutput("No command configured for " + project.displayName());
            return;
        }

        boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
        List<String> commandLine = isWindows
                ? List.of("cmd.exe", "/c", command)
                : List.of("bash", "-lc", command);

        ProcessBuilder processBuilder = new ProcessBuilder(commandLine);
        processBuilder.directory(ROOT.toFile());
        processBuilder.redirectErrorStream(true);

        appendOutput("Running command: " + command);

        try {
            Process process = processBuilder.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    appendOutput(line);
                }
            }
            int exitCode = process.waitFor();
            appendOutput("Process finished with exit code " + exitCode);
        } catch (IOException ex) {
            appendOutput("Failed to start process: " + ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            appendOutput("Execution interrupted.");
        }
    }

    private void appendOutput(String message) {
        SwingUtilities.invokeLater(() -> {
            if (!outputArea.getText().isEmpty()) {
                outputArea.append(System.lineSeparator());
            }
            outputArea.append(message);
            outputArea.setCaretPosition(outputArea.getDocument().getLength());
        });
    }

    private void updateDetails(Project project) {
        if (project == null) {
            detailsArea.setText("");
            runButton.setEnabled(false);
            return;
        }

        StringBuilder builder = new StringBuilder();
        builder.append(project.displayName()).append(System.lineSeparator()).append(System.lineSeparator());

        if (project.description() != null && !project.description().isBlank()) {
            wrapText(project.description()).forEach(line -> builder.append(line).append(System.lineSeparator()));
            builder.append(System.lineSeparator());
        }

        builder.append("Type: ").append(project.type().name().toLowerCase(Locale.ROOT)).append(System.lineSeparator());
        if (project.command() != null && !project.command().isBlank()) {
            builder.append("Command: ").append(project.command()).append(System.lineSeparator());
        }
        if (!project.requirements().isEmpty()) {
            builder.append("Requirements: ").append(String.join(", ", project.requirements())).append(System.lineSeparator());
        }
        builder.append("Metadata: ").append(ROOT.relativize(project.metadataPath()).toString());

        detailsArea.setText(builder.toString());
        detailsArea.setCaretPosition(0);
        runButton.setEnabled(true);
    }

    private static List<String> wrapText(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        int width = 70;
        List<String> lines = new ArrayList<>();
        String remaining = text.trim();
        while (!remaining.isEmpty()) {
            if (remaining.length() <= width) {
                lines.add(remaining);
                break;
            }
            int breakIndex = remaining.lastIndexOf(' ', width);
            if (breakIndex <= 0) {
                breakIndex = width;
            }
            lines.add(remaining.substring(0, breakIndex));
            remaining = remaining.substring(breakIndex).trim();
        }
        return lines;
    }

    private List<Project> discoverProjects() {
        try (Stream<Path> stream = Files.walk(ROOT, MAX_SCAN_DEPTH, FileVisitOption.FOLLOW_LINKS)) {
            return stream
                    .filter(path -> path.getFileName().toString().equals(PROJECT_FILE_NAME))
                    .map(this::parseProjectFile)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .sorted((a, b) -> a.displayName().compareToIgnoreCase(b.displayName()))
                    .collect(Collectors.toUnmodifiableList());
        } catch (IOException ex) {
            appendOutput("Error while scanning projects: " + ex.getMessage());
            return List.of();
        }
    }

    private Optional<Project> parseProjectFile(Path path) {
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            Object parsed = new JsonParser(content).parse();
            if (!(parsed instanceof Map<?, ?> map)) {
                appendOutput("Skipping " + relativize(path) + ": JSON root is not an object.");
                return Optional.empty();
            }
            return Project.fromMap(map, path);
        } catch (IOException ex) {
            appendOutput("Skipping " + relativize(path) + ": " + ex.getMessage());
        } catch (RuntimeException ex) {
            appendOutput("Skipping " + relativize(path) + " due to JSON error: " + ex.getMessage());
        }
        return Optional.empty();
    }

    private static String relativize(Path path) {
        return ROOT.relativize(path.toAbsolutePath()).toString();
    }

    private class ProjectSelectionListener implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) {
                updateDetails(projectList.getSelectedValue());
            }
        }
    }

    private static class ProjectCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Project project) {
                setText(project.displayName());
            }
            setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
            return this;
        }
    }

    private record Project(
            String id,
            String name,
            String description,
            ProjectType type,
            String command,
            List<String> requirements,
            Path metadataPath
    ) {
        String displayName() {
            if (name != null && !name.isBlank()) {
                return name;
            }
            if (id != null && !id.isBlank()) {
                return id;
            }
            return metadataPath.getParent() != null ? metadataPath.getParent().getFileName().toString() : metadataPath.toString();
        }

        static Optional<Project> fromMap(Map<?, ?> map, Path source) {
            String id = getAsString(map, "id");
            String name = getAsString(map, "name");
            String description = getAsString(map, "description");
            String typeValue = Optional.ofNullable(getAsString(map, "type")).orElse("shell");
            String command = getAsString(map, "command");
            List<String> requirements = getAsStringList(map.get("requirements"));

            ProjectType type;
            try {
                type = ProjectType.fromString(typeValue);
            } catch (IllegalArgumentException ex) {
                return Optional.empty();
            }

            return Optional.of(new Project(id, name, description, type, command, requirements, source));
        }
    }

    private enum ProjectType {
        SHELL, BROWSER;

        static ProjectType fromString(String value) {
            return switch (value.toLowerCase(Locale.ROOT)) {
                case "shell", "script", "command" -> SHELL;
                case "browser", "web" -> BROWSER;
                default -> throw new IllegalArgumentException("Unsupported project type: " + value);
            };
        }
    }

    private static List<String> getAsStringList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
            return Collections.unmodifiableList(result);
        }
        return List.of();
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic(KeyEvent.VK_V);

        JMenuItem increaseFont = new JMenuItem("Increase UI scale");
        increaseFont.setMnemonic(KeyEvent.VK_I);
        increaseFont.addActionListener(e -> adjustGlobalFontScale(1.12f));

        JMenuItem decreaseFont = new JMenuItem("Decrease UI scale");
        decreaseFont.setMnemonic(KeyEvent.VK_D);
        decreaseFont.addActionListener(e -> adjustGlobalFontScale(1f / 1.12f));

        JMenuItem resetFont = new JMenuItem("Reset UI scale");
        resetFont.setMnemonic(KeyEvent.VK_R);
        resetFont.addActionListener(e -> adjustGlobalFontScale(1.0f, true));

        viewMenu.add(increaseFont);
        viewMenu.add(decreaseFont);
        viewMenu.addSeparator();
        viewMenu.add(resetFont);

        menuBar.add(viewMenu);
        return menuBar;
    }

    private float currentScale = 1.0f;

    private void adjustGlobalFontScale(float factor) {
        adjustGlobalFontScale(factor, false);
    }

    private void adjustGlobalFontScale(float factor, boolean reset) {
        currentScale = reset ? 1.0f : Math.max(0.6f, Math.min(2.4f, currentScale * factor));
        refreshScaledUI();
    }

    private void refreshScaledUI() {
        applyScaleToUIDefaults(currentScale);
        applyScaleToComponentTree(frame.getJMenuBar(), currentScale);
        applyScaleToComponentTree(frame.getContentPane(), currentScale);
        if (frame != null) {
            SwingUtilities.updateComponentTreeUI(frame);
            frame.invalidate();
            frame.validate();
            frame.repaint();
        }
    }

    private void registerAccelerators(JFrame frame) {
        if (frame == null) {
            return;
        }
        JRootPane rootPane = frame.getRootPane();
        if (rootPane == null) {
            return;
        }

        int shortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();

        KeyStroke[] increaseStrokes = new KeyStroke[]{
                KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, shortcutMask | KeyEvent.SHIFT_DOWN_MASK),
                KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, shortcutMask),
                KeyStroke.getKeyStroke(KeyEvent.VK_ADD, shortcutMask)
        };
        KeyStroke[] decreaseStrokes = new KeyStroke[]{
                KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, shortcutMask),
                KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, shortcutMask)
        };

        for (KeyStroke stroke : increaseStrokes) {
            if (stroke != null) {
                inputMap.put(stroke, "increaseScale");
            }
        }
        for (KeyStroke stroke : decreaseStrokes) {
            if (stroke != null) {
                inputMap.put(stroke, "decreaseScale");
            }
        }

        actionMap.put("increaseScale", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                adjustGlobalFontScale(1.12f);
            }
        });

        actionMap.put("decreaseScale", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                adjustGlobalFontScale(1f / 1.12f);
            }
        });
    }

    private void registerComponentTree(Component component) {
        if (component == null) {
            return;
        }

        if (component instanceof JComponent jComponent) {
            if (jComponent.getClientProperty(BASE_FONT_PROPERTY) == null) {
                Font font = jComponent.getFont();
                if (font != null) {
                    jComponent.putClientProperty(BASE_FONT_PROPERTY, font);
                }
            }

            if (jComponent.getClientProperty(BASE_BORDER_INSETS_PROPERTY) == null) {
                if (jComponent.getBorder() instanceof EmptyBorder emptyBorder) {
                    Insets insets = emptyBorder.getBorderInsets(jComponent);
                    jComponent.putClientProperty(BASE_BORDER_INSETS_PROPERTY, new Insets(insets.top, insets.left, insets.bottom, insets.right));
                }
            }
        }

        if (component instanceof AbstractButton button) {
            if (button.getClientProperty(BASE_MARGIN_PROPERTY) == null) {
                Insets margin = button.getMargin();
                if (margin != null) {
                    button.putClientProperty(BASE_MARGIN_PROPERTY, new Insets(margin.top, margin.left, margin.bottom, margin.right));
                }
            }
        }

        if (component instanceof JSplitPane splitPane) {
            if (splitPane.getClientProperty(BASE_DIVIDER_PROPERTY) == null) {
                splitPane.putClientProperty(BASE_DIVIDER_PROPERTY, splitPane.getDividerSize());
            }
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                registerComponentTree(child);
            }
        }
        if (component instanceof JMenuBar menuBar) {
            for (int i = 0; i < menuBar.getMenuCount(); i++) {
                registerComponentTree(menuBar.getMenu(i));
            }
        }
        if (component instanceof JMenu menu) {
            for (int i = 0; i < menu.getItemCount(); i++) {
                registerComponentTree(menu.getItem(i));
            }
        }
    }

    private void applyScaleToComponentTree(Component component, float scale) {
        if (component == null) {
            return;
        }

        if (component instanceof JComponent jComponent) {
            Font baseFont = (Font) jComponent.getClientProperty(BASE_FONT_PROPERTY);
            if (baseFont != null) {
                jComponent.setFont(baseFont.deriveFont(baseFont.getSize2D() * scale));
            }

            Insets baseBorderInsets = (Insets) jComponent.getClientProperty(BASE_BORDER_INSETS_PROPERTY);
            if (baseBorderInsets != null) {
                jComponent.setBorder(new EmptyBorder(scaleValue(baseBorderInsets.top, scale),
                        scaleValue(baseBorderInsets.left, scale),
                        scaleValue(baseBorderInsets.bottom, scale),
                        scaleValue(baseBorderInsets.right, scale)));
            }
        }

        if (component instanceof AbstractButton button) {
            Insets baseMargin = (Insets) button.getClientProperty(BASE_MARGIN_PROPERTY);
            if (baseMargin != null) {
                button.setMargin(new Insets(scaleValue(baseMargin.top, scale),
                        scaleValue(baseMargin.left, scale),
                        scaleValue(baseMargin.bottom, scale),
                        scaleValue(baseMargin.right, scale)));
            }
        }

        if (component instanceof JSplitPane splitPane) {
            Integer baseDivider = (Integer) splitPane.getClientProperty(BASE_DIVIDER_PROPERTY);
            if (baseDivider != null) {
                splitPane.setDividerSize(scaleValue(baseDivider, scale));
            }
        }

        if (component instanceof JList<?> list) {
            list.setFixedCellHeight(-1);
            list.setFixedCellWidth(-1);
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                applyScaleToComponentTree(child, scale);
            }
        }
        if (component instanceof JMenuBar menuBar) {
            for (int i = 0; i < menuBar.getMenuCount(); i++) {
                JMenu menu = menuBar.getMenu(i);
                applyScaleToComponentTree(menu, scale);
            }
        }
        if (component instanceof JMenu menu) {
            for (int i = 0; i < menu.getItemCount(); i++) {
                JMenuItem item = menu.getItem(i);
                if (item != null) {
                    applyScaleToComponentTree(item, scale);
                }
            }
        }
    }

    private static void captureBaseDefaults() {
        if (defaultsCaptured) {
            return;
        }
        UIDefaults defaults = UIManager.getLookAndFeelDefaults();
        for (Object key : defaults.keySet()) {
            Object value = defaults.get(key);
            if (value instanceof Font font) {
                BASE_DEFAULT_FONTS.put(key, font);
            } else if (value instanceof Insets insets) {
                BASE_DEFAULT_INSETS.put(key, new Insets(insets.top, insets.left, insets.bottom, insets.right));
            } else if (value instanceof Dimension dimension) {
                BASE_DEFAULT_DIMENSIONS.put(key, new Dimension(dimension));
            }
        }
        defaultsCaptured = true;
    }

    private static void applyScaleToUIDefaults(float scale) {
        UIDefaults defaults = UIManager.getLookAndFeelDefaults();
        BASE_DEFAULT_FONTS.forEach((key, font) -> defaults.put(key, font.deriveFont(font.getSize2D() * scale)));
        BASE_DEFAULT_INSETS.forEach((key, insets) -> defaults.put(key, new InsetsUIResource(
                scaleValue(insets.top, scale),
                scaleValue(insets.left, scale),
                scaleValue(insets.bottom, scale),
                scaleValue(insets.right, scale)
        )));
        BASE_DEFAULT_DIMENSIONS.forEach((key, dimension) -> defaults.put(key, new DimensionUIResource(
                scaleValue(dimension.width, scale),
                scaleValue(dimension.height, scale)
        )));
    }

    private static int scaleValue(int base, float scale) {
        if (base == 0) {
            return 0;
        }
        int scaled = Math.round(base * scale);
        return Math.max(1, scaled);
    }

    @SuppressWarnings("unchecked")
    private static String getAsString(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private static final class JsonParser {
        private final String source;
        private int index;

        JsonParser(String source) {
            this.source = source;
            this.index = 0;
        }

        Object parse() {
            skipWhitespace();
            Object value = parseValue();
            skipWhitespace();
            if (!isEnd()) {
                throw new ParseException("Unexpected trailing characters at position " + index);
            }
            return value;
        }

        private Object parseValue() {
            skipWhitespace();
            if (isEnd()) {
                throw new ParseException("Unexpected end of input");
            }
            char current = peek();
            return switch (current) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't', 'f' -> parseBoolean();
                case 'n' -> parseNull();
                default -> {
                    if (current == '-' || Character.isDigit(current)) {
                        yield parseNumber();
                    }
                    throw new ParseException("Unexpected character '" + current + "' at position " + index);
                }
            };
        }

        private Map<String, Object> parseObject() {
            expect('{');
            skipWhitespace();
            Map<String, Object> result = new LinkedHashMap<>();
            if (consumeIf('}')) {
                return result;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                skipWhitespace();
                Object value = parseValue();
                result.put(key, value);
                skipWhitespace();
                if (consumeIf('}')) {
                    break;
                }
                expect(',');
            }
            return result;
        }

        private List<Object> parseArray() {
            expect('[');
            skipWhitespace();
            List<Object> result = new ArrayList<>();
            if (consumeIf(']')) {
                return result;
            }
            while (true) {
                result.add(parseValue());
                skipWhitespace();
                if (consumeIf(']')) {
                    break;
                }
                expect(',');
            }
            return result;
        }

        private String parseString() {
            expect('"');
            StringBuilder builder = new StringBuilder();
            while (!isEnd()) {
                char current = next();
                if (current == '"') {
                    return builder.toString();
                }
                if (current == '\\') {
                    if (isEnd()) {
                        throw new ParseException("Unterminated escape sequence at position " + index);
                    }
                    char escaped = next();
                    switch (escaped) {
                        case '"', '\\', '/' -> builder.append(escaped);
                        case 'b' -> builder.append('\b');
                        case 'f' -> builder.append('\f');
                        case 'n' -> builder.append('\n');
                        case 'r' -> builder.append('\r');
                        case 't' -> builder.append('\t');
                        case 'u' -> builder.append(parseUnicodeEscape());
                        default -> throw new ParseException("Invalid escape sequence '\\" + escaped + "' at position " + index);
                    }
                } else {
                    builder.append(current);
                }
            }
            throw new ParseException("Unterminated string literal");
        }

        private char parseUnicodeEscape() {
            int codePoint = 0;
            for (int i = 0; i < 4; i++) {
                if (isEnd()) {
                    throw new ParseException("Incomplete unicode escape sequence");
                }
                char c = next();
                codePoint <<= 4;
                if (c >= '0' && c <= '9') {
                    codePoint += c - '0';
                } else if (c >= 'a' && c <= 'f') {
                    codePoint += 10 + (c - 'a');
                } else if (c >= 'A' && c <= 'F') {
                    codePoint += 10 + (c - 'A');
                } else {
                    throw new ParseException("Invalid unicode escape character '" + c + "'");
                }
            }
            return (char) codePoint;
        }

        private Object parseNumber() {
            int start = index;
            if (peek() == '-') {
                next();
            }
            readDigits("number");
            if (!isEnd() && peek() == '.') {
                next();
                readDigits("fraction");
            }
            if (!isEnd() && (peek() == 'e' || peek() == 'E')) {
                next();
                if (!isEnd() && (peek() == '+' || peek() == '-')) {
                    next();
                }
                readDigits("exponent");
            }
            String number = source.substring(start, index);
            if (number.contains(".") || number.contains("e") || number.contains("E")) {
                return Double.parseDouble(number);
            }
            try {
                return Long.parseLong(number);
            } catch (NumberFormatException ex) {
                return Double.parseDouble(number);
            }
        }

        private Boolean parseBoolean() {
            if (match("true")) {
                return Boolean.TRUE;
            }
            if (match("false")) {
                return Boolean.FALSE;
            }
            throw new ParseException("Invalid literal at position " + index);
        }

        private Object parseNull() {
            if (match("null")) {
                return null;
            }
            throw new ParseException("Invalid literal at position " + index);
        }

        private void readDigits(String context) {
            if (isEnd() || !Character.isDigit(peek())) {
                throw new ParseException("Expected digit in " + context + " at position " + index);
            }
            while (!isEnd() && Character.isDigit(peek())) {
                next();
            }
        }

        private void skipWhitespace() {
            while (!isEnd() && Character.isWhitespace(peek())) {
                next();
            }
        }

        private boolean consumeIf(char expected) {
            if (!isEnd() && peek() == expected) {
                next();
                return true;
            }
            return false;
        }

        private void expect(char expected) {
            if (isEnd() || peek() != expected) {
                throw new ParseException("Expected '" + expected + "' at position " + index);
            }
            next();
        }

        private boolean match(String literal) {
            if (source.startsWith(literal, index)) {
                index += literal.length();
                return true;
            }
            return false;
        }

        private char next() {
            return source.charAt(index++);
        }

        private char peek() {
            return source.charAt(index);
        }

        private boolean isEnd() {
            return index >= source.length();
        }

        private static final class ParseException extends RuntimeException {
            ParseException(String message) {
                super(message);
            }
        }
    }
}
