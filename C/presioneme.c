/*
 * Compilación recomendada:
 *   gcc presioneme.c -o presioneme -lX11
 */

#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <stdbool.h>
#include <stdio.h>
#include <string.h>

typedef struct {
    int x;
    int y;
    int width;
    int height;
} Rect;

static void draw_button(Display *display, Window window, GC gc, XFontStruct *font,
                        const Rect *bounds, const char *label) {
    XDrawRectangle(display, window, gc, bounds->x, bounds->y, bounds->width, bounds->height);

    if (font && label) {
        int label_width = XTextWidth(font, label, (int)strlen(label));
        int label_x = bounds->x + (bounds->width - label_width) / 2;
        int label_y = bounds->y + (bounds->height + (font->ascent - font->descent)) / 2;
        XDrawString(display, window, gc, label_x, label_y, label, (int)strlen(label));
    }
}

static void draw_main_window(Display *display, Window window, GC gc, XFontStruct *font,
                             int width, int height, const Rect *button) {
    const char *title = "Aplicación de Botón";

    (void)height;

    XClearWindow(display, window);

    if (font) {
        int title_width = XTextWidth(font, title, (int)strlen(title));
        XDrawString(display, window, gc, (width - title_width) / 2, 40, title, (int)strlen(title));
    }

    draw_button(display, window, gc, font, button, "Presioneme");
    XFlush(display);
}

static void draw_dialog(Display *display, Window dialog, GC gc, XFontStruct *font,
                        int width, int height, const Rect *button) {
    const char *message = "\302\241Has presionado el bot\303\263n!";

    XClearWindow(display, dialog);

    if (font) {
        int message_width = XTextWidth(font, message, (int)strlen(message));
        XDrawString(display, dialog, gc, (width - message_width) / 2, height / 2 - 20,
                    message, (int)strlen(message));
    }

    draw_button(display, dialog, gc, font, button, "Aceptar");
    XFlush(display);
}

static bool point_in_rect(int x, int y, const Rect *rect) {
    return x >= rect->x && x <= rect->x + rect->width &&
           y >= rect->y && y <= rect->y + rect->height;
}

int main(void) {
    Display *display = XOpenDisplay(NULL);
    if (!display) {
        fprintf(stderr, "No se pudo abrir la pantalla X.\n");
        return 1;
    }

    int screen = DefaultScreen(display);
    Window root = RootWindow(display, screen);
    unsigned long white = WhitePixel(display, screen);
    unsigned long black = BlackPixel(display, screen);

    int width = 480;
    int height = 320;
    int screen_width = DisplayWidth(display, screen);
    int screen_height = DisplayHeight(display, screen);

    Window window = XCreateSimpleWindow(display, root, 0, 0, (unsigned int)width, (unsigned int)height,
                                        1, black, white);
    XStoreName(display, window, "Presioneme");

    Atom wm_delete = XInternAtom(display, "WM_DELETE_WINDOW", False);
    XSetWMProtocols(display, window, &wm_delete, 1);

    XSelectInput(display, window, ExposureMask | ButtonPressMask | StructureNotifyMask);
    XMapWindow(display, window);

    GC gc = XCreateGC(display, window, 0, NULL);
    XSetBackground(display, gc, white);
    XSetForeground(display, gc, black);

    XFontStruct *font = XLoadQueryFont(display, "-misc-fixed-*-*-*-*-18-*-*-*-*-*-*-*");
    if (!font) {
        font = XLoadQueryFont(display, "fixed");
    }
    if (font) {
        XSetFont(display, gc, font->fid);
    }

    bool running = true;
    bool dialog_visible = false;
    Window dialog_window = 0;
    GC dialog_gc = 0;
    Rect main_button = {0};
    Rect dialog_button = {0};
    int dialog_width = 360;
    int dialog_height = 180;

    main_button.width = 180;
    main_button.height = 56;
    main_button.x = (width - main_button.width) / 2;
    main_button.y = (height - main_button.height) / 2;

    XEvent event;
    while (running) {
        XNextEvent(display, &event);
        switch (event.type) {
        case Expose: {
            if (event.xexpose.window == window) {
                draw_main_window(display, window, gc, font, width, height, &main_button);
            } else if (dialog_visible && event.xexpose.window == dialog_window) {
                draw_dialog(display, dialog_window, dialog_gc ? dialog_gc : gc, font,
                            dialog_width, dialog_height, &dialog_button);
            }
            break;
        }
        case ConfigureNotify:
            if (event.xconfigure.window == window) {
                width = event.xconfigure.width;
                height = event.xconfigure.height;
                main_button.x = (width - main_button.width) / 2;
                main_button.y = (height - main_button.height) / 2;
                draw_main_window(display, window, gc, font, width, height, &main_button);
            } else if (dialog_visible && event.xconfigure.window == dialog_window) {
                dialog_width = event.xconfigure.width;
                dialog_height = event.xconfigure.height;
                dialog_button.width = 120;
                dialog_button.height = 44;
                dialog_button.x = (dialog_width - dialog_button.width) / 2;
                dialog_button.y = dialog_height - dialog_button.height - 30;
                draw_dialog(display, dialog_window, dialog_gc ? dialog_gc : gc, font,
                            dialog_width, dialog_height, &dialog_button);
            }
            break;
        case ButtonPress: {
            if (event.xbutton.window == window) {
                if (point_in_rect(event.xbutton.x, event.xbutton.y, &main_button) && !dialog_visible) {
                    dialog_window = XCreateSimpleWindow(display, root,
                                                       0, 0,
                                                       (unsigned int)dialog_width,
                                                       (unsigned int)dialog_height,
                                                       1, black, white);
                    XStoreName(display, dialog_window, "Mensaje");
                    XSetWMProtocols(display, dialog_window, &wm_delete, 1);
                    XSelectInput(display, dialog_window,
                                 ExposureMask | ButtonPressMask | StructureNotifyMask);
                    int dialog_x = (screen_width - dialog_width) / 2;
                    int dialog_y = (screen_height - dialog_height) / 2;
                    XMoveWindow(display, dialog_window, dialog_x < 0 ? 0 : dialog_x,
                                dialog_y < 0 ? 0 : dialog_y);
                    XMapWindow(display, dialog_window);

                    dialog_gc = XCreateGC(display, dialog_window, 0, NULL);
                    XSetBackground(display, dialog_gc, white);
                    XSetForeground(display, dialog_gc, black);
                    if (font) {
                        XSetFont(display, dialog_gc, font->fid);
                    }

                    dialog_button.width = 120;
                    dialog_button.height = 44;
                    dialog_button.x = (dialog_width - dialog_button.width) / 2;
                    dialog_button.y = dialog_height - dialog_button.height - 30;

                    dialog_visible = true;
                }
            } else if (dialog_visible && event.xbutton.window == dialog_window) {
                if (point_in_rect(event.xbutton.x, event.xbutton.y, &dialog_button)) {
                    XDestroyWindow(display, dialog_window);
                    if (dialog_gc) {
                        XFreeGC(display, dialog_gc);
                        dialog_gc = 0;
                    }
                    dialog_visible = false;
                }
            }
            break;
        }
        case ClientMessage:
            if ((Atom)event.xclient.data.l[0] == wm_delete) {
                if (dialog_visible && event.xclient.window == dialog_window) {
                    XDestroyWindow(display, dialog_window);
                    if (dialog_gc) {
                        XFreeGC(display, dialog_gc);
                        dialog_gc = 0;
                    }
                    dialog_visible = false;
                } else if (event.xclient.window == window) {
                    running = false;
                }
            }
            break;
        default:
            break;
        }
    }

    if (dialog_visible) {
        XDestroyWindow(display, dialog_window);
        if (dialog_gc) {
            XFreeGC(display, dialog_gc);
        }
    }

    if (font) {
        XFreeFont(display, font);
    }
    XFreeGC(display, gc);
    XDestroyWindow(display, window);
    XCloseDisplay(display);

    return 0;
}
