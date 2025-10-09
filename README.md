# Codex tests

This repository contains exercises and utilities built by the Codex team, including a Java Swing HTTP client and a web-based Snake game.

## Run the projects with the menu
Use the Java launcher to list and execute any of the available projects:
```bash
java RunProject.java
```
The menu enumerates projects discovered through their metadata files and executes them with the required tooling (Maven/JDK for `envio`, default browser for `snake`). The command works with Java 17+ and requires no additional dependencies.

### Graphical launcher (Swing)
If you prefer a graphical interface, run:
```bash
javac RunGui.java
java RunGui
```
This opens a dark-themed Swing UI with the same project list, allowing you to launch commands or open browser projects with a button click. A graphical environment (X11/Wayland on Linux, Desktop on macOS/Windows) is required.

## Project metadata
Every runnable project exposes a `project.json` file located in its directory. The file describes the project name, a short summary, optional requirements, and how it should be launched. Example:
```json
{
  "id": "envio",
  "name": "Envio HTTP Client",
  "description": "Java Swing app to inspect HTTP requests and JSON responses.",
  "type": "shell",
  "command": "./java/envio/run.sh",
  "requirements": ["Maven 3.x", "JDK 17+"]
}
```
To add a new project to the menu, drop a similar `project.json` file into the project folder. The `command` should be runnable from the repository root; for browser projects, use `"type": "browser"` and point `command` to the file or URL that should be opened.
