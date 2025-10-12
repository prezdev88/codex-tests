# Presioneme

Aplicación gráfica sencilla en C que utiliza Xlib para mostrar un botón "Presioneme".

## Compilación

En la carpeta `C/` ejecute:

```bash
make
```

Esto genera el ejecutable `presioneme` compilando con `gcc presioneme.c -o presioneme -lX11`.

Si desea compilar manualmente sin `make`, asegúrese de incluir la bandera de enlace a Xlib:

```bash
gcc presioneme.c -o presioneme -lX11
```

## Ejecución

Tras compilar, ejecute el binario:

```bash
./presioneme
```

Se abrirá una ventana con un botón. Al pulsarlo se mostrará el mensaje "¡Botón presionado!" dentro de la misma ventana.
