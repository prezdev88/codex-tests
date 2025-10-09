# Codex tests

Colección de ejercicios y utilidades que combinan una aplicación Java Swing y un juego web estilo Snake.

## Contenido del repositorio
- `java/envio`: cliente HTTP con interfaz Swing que permite enviar peticiones, visualizar el JSON formateado y ajustar preferencias de UI.
- `html/snake`: implementación del juego Snake en HTML/JS con selector de idioma, ajustes de velocidad y HUD optimizado.

## Requisitos
- JDK 17 o superior (según `java/envio/pom.xml`).
- Maven 3.x disponible en la línea de comandos.

## Ejecutar el proyecto Java `envio`
### Opción rápida
```bash
./java/envio/run.sh
```
El script limpia y empaqueta el proyecto con Maven, y luego lanza la aplicación mediante `exec:java`. Al finalizar el build se abrirá la ventana Swing (requiere entorno gráfico).

### Comandos manuales
Si prefieres ejecutar los pasos por separado:
```bash
mvn -f java/envio/pom.xml clean package
mvn -f java/envio/pom.xml exec:java
```

## Menú general de proyectos
Puedes lanzar cualquiera de los proyectos con el menú interactivo:
```bash
./run-project.sh
```
La opción 1 ejecuta el cliente HTTP (requiere Maven y JDK 17).  
La opción 2 abre `html/snake/game.html` en tu navegador predeterminado.

## Probar el juego Snake
Abre `html/snake/game.html` en tu navegador para jugar la versión con selector de idioma, ajustes de velocidad y HUD mejorado.
