import java.awt.Desktop;
import java.io.IOException;
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
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RunProject {

    private static final Path ROOT = Paths.get("").toAbsolutePath();
    private static final String PROJECT_FILE_NAME = "project.json";
    private static final int MAX_SCAN_DEPTH = 6;

    public static void main(String[] args) {
        List<Project> projects = discoverProjects();
        if (projects.isEmpty()) {
            System.out.printf("No projects found. Add %s files to project directories.%n", PROJECT_FILE_NAME);
            return;
        }

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.println();
                printMenu(projects);
                System.out.print("Choose an option: ");
                String choice = scanner.nextLine().trim();

                if (choice.equalsIgnoreCase("q")) {
                    System.out.println("Bye!");
                    return;
                }

                if (!choice.matches("\\d+")) {
                    System.out.println("Invalid choice. Please enter a number or 'q'.");
                    continue;
                }

                int index = Integer.parseInt(choice) - 1;
                if (index < 0 || index >= projects.size()) {
                    System.out.println("Choice out of range. Try again.");
                    continue;
                }

                executeProject(projects.get(index));
            }
        }
    }

    private static List<Project> discoverProjects() {
        try (Stream<Path> stream = Files.walk(ROOT, MAX_SCAN_DEPTH, FileVisitOption.FOLLOW_LINKS)) {
            return stream
                    .filter(path -> path.getFileName().toString().equals(PROJECT_FILE_NAME))
                    .map(RunProject::parseProjectFile)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .sorted((a, b) -> a.displayName().compareToIgnoreCase(b.displayName()))
                    .collect(Collectors.toUnmodifiableList());
        } catch (IOException ex) {
            System.out.println("Error while scanning projects: " + ex.getMessage());
            return List.of();
        }
    }

    private static Optional<Project> parseProjectFile(Path path) {
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            Object parsed = new JsonParser(content).parse();
            if (!(parsed instanceof Map<?, ?> map)) {
                System.out.printf("Skipping %s: JSON root is not an object.%n", relativize(path));
                return Optional.empty();
            }
            return Project.fromMap(map, path);
        } catch (IOException ex) {
            System.out.printf("Skipping %s: %s%n", relativize(path), ex.getMessage());
        } catch (RuntimeException ex) {
            System.out.printf("Skipping %s due to JSON error: %s%n", relativize(path), ex.getMessage());
        }
        return Optional.empty();
    }

    private static void printMenu(List<Project> projects) {
        System.out.println("Available projects:");
        for (int i = 0; i < projects.size(); i++) {
            Project project = projects.get(i);
            System.out.printf("  %d) %s%n", i + 1, project.displayName());
            wrapText(project.description()).forEach(line -> System.out.printf("      %s%n", line));
            if (!project.requirements().isEmpty()) {
                System.out.printf("      Requirements: %s%n", String.join(", ", project.requirements()));
            }
            if (project.command() != null && !project.command().isBlank()) {
                System.out.printf("      Command: %s%n", project.command());
            }
            System.out.println();
        }
        System.out.println("  q) Quit");
    }

    private static void executeProject(Project project) {
        switch (project.type()) {
            case BROWSER -> openInBrowser(project);
            case SHELL -> runShellCommand(project);
        }
    }

    private static void openInBrowser(Project project) {
        String command = project.command();
        if (command == null || command.isBlank()) {
            System.out.printf("No path configured for %s.%n", project.displayName());
            return;
        }

        try {
            java.net.URI uri;
            if (command.startsWith("http://") || command.startsWith("https://") || command.startsWith("file:")) {
                uri = java.net.URI.create(command);
            } else {
                Path resolved = ROOT.resolve(command).normalize();
                if (!Files.exists(resolved)) {
                    System.out.printf("File not found: %s%n", resolved);
                    return;
                }
                uri = resolved.toUri();
            }

            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                System.out.printf("Desktop browsing not supported. Open manually: %s%n", uri);
                return;
            }

            System.out.printf("Opening %s...%n", project.displayName());
            Desktop.getDesktop().browse(uri);
        } catch (IOException | IllegalArgumentException ex) {
            System.out.printf("Failed to open browser for %s: %s%n", project.displayName(), ex.getMessage());
        }
    }

    private static void runShellCommand(Project project) {
        String command = project.command();
        if (command == null || command.isBlank()) {
            System.out.printf("No command configured for %s.%n", project.displayName());
            return;
        }

        boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
        List<String> commandLine = isWindows
                ? List.of("cmd.exe", "/c", command)
                : List.of("bash", "-lc", command);

        System.out.printf("Running %s...%n", project.displayName());
        ProcessBuilder processBuilder = new ProcessBuilder(commandLine);
        processBuilder.directory(ROOT.toFile());
        processBuilder.inheritIO();

        try {
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.out.printf("%s finished with exit code %d.%n", project.displayName(), exitCode);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            System.out.printf("Execution interrupted while running %s.%n", project.displayName());
        } catch (IOException ex) {
            System.out.printf("Failed to run %s: %s%n", project.displayName(), ex.getMessage());
        }
    }

    private static String relativize(Path path) {
        return ROOT.relativize(path.toAbsolutePath()).toString();
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
                System.out.printf("Skipping %s: %s%n", source, ex.getMessage());
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

        static final class ParseException extends RuntimeException {
            ParseException(String message) {
                super(message);
            }
        }
    }
}
