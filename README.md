# Codex tests

## Ejecutar el proyecto Java `envio`

0.- All in one

   ```
   mvn -f java/envio/pom.xml clean package && mvn -f java/envio/po
   m.xml exec:java
   ```

1. Compila las dependencias y genera el artefacto:

   ```bash
   mvn -f java/envio/pom.xml clean package
   ```

2. Ejecuta la aplicación Swing con el `exec-maven-plugin`:

   ```bash
   mvn -f java/envio/pom.xml exec:java
   ```

Ambos comandos requieren JDK 17 o superior, tal como se define en el `pom.xml` del módulo.
