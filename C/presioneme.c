#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <stdbool.h>
#include <stdio.h>
#include <string.h>

static void draw_ui(Display *display, Window window, GC gc, XFontStruct *font,
                    int width, int height, bool message_visible) {
    const char *title = "Aplicación de Botón";
    const char *button_label = "Presioneme";
    const char *message = "\302\241Bot\303\263n presionado!";

    int button_width = 160;
    int button_height = 48;
    int button_x = (width - button_width) / 2;
    int button_y = (height - button_height) / 2;

    XClearWindow(display, window);

    XSetForeground(display, gc, BlackPixel(display, DefaultScreen(display)));

    if (font) {
        int title_width = XTextWidth(font, title, (int)strlen(title));
        XDrawString(display, window, gc, (width - title_width) / 2, 40, title, (int)strlen(title));
    }

    XDrawRectangle(display, window, gc, button_x, button_y, button_width, button_height);

    if (font) {
        int label_width = XTextWidth(font, button_label, (int)strlen(button_label));
        int label_x = button_x + (button_width - label_width) / 2;
        int label_y = button_y + (button_height + (font->ascent - font->descent)) / 2;
        XDrawString(display, window, gc, label_x, label_y, button_label, (int)strlen(button_label));
    }

    if (message_visible && font) {
        int message_width = XTextWidth(font, message, (int)strlen(message));
        int message_y = button_y + button_height + 60;
        XDrawString(display, window, gc, (width - message_width) / 2, message_y, message, (int)strlen(message));
    }

    XFlush(display);
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

    bool message_visible = false;
    bool running = true;

    XEvent event;
    while (running) {
        XNextEvent(display, &event);
        switch (event.type) {
        case Expose:
            draw_ui(display, window, gc, font, width, height, message_visible);
            break;
        case ConfigureNotify:
            width = event.xconfigure.width;
            height = event.xconfigure.height;
            draw_ui(display, window, gc, font, width, height, message_visible);
            break;
        case ButtonPress: {
            int button_width = 160;
            int button_height = 48;
            int button_x = (width - button_width) / 2;
            int button_y = (height - button_height) / 2;
            int click_x = event.xbutton.x;
            int click_y = event.xbutton.y;
            if (click_x >= button_x && click_x <= button_x + button_width &&
                click_y >= button_y && click_y <= button_y + button_height) {
                message_visible = true;
                draw_ui(display, window, gc, font, width, height, message_visible);
            }
            break;
        }
        case ClientMessage:
            if ((Atom)event.xclient.data.l[0] == wm_delete) {
                running = false;
            }
            break;
        default:
            break;
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
