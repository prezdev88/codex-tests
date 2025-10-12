#include <gtk/gtk.h>

static void on_button_clicked(GtkWidget *button, gpointer user_data) {
    GtkWindow *parent = GTK_WINDOW(user_data);
    GtkWidget *dialog = gtk_message_dialog_new(
        parent,
        GTK_DIALOG_MODAL,
        GTK_MESSAGE_INFO,
        GTK_BUTTONS_OK,
        "\302\241Bot\303\263n presionado!");
    gtk_window_set_title(GTK_WINDOW(dialog), "Mensaje");
    gtk_dialog_run(GTK_DIALOG(dialog));
    gtk_widget_destroy(dialog);
}

int main(int argc, char *argv[]) {
    gtk_init(&argc, &argv);

    GtkWidget *window = gtk_window_new(GTK_WINDOW_TOPLEVEL);
    gtk_window_set_title(GTK_WINDOW(window), "Aplicaci\303\263n de Bot\303\263n");
    gtk_container_set_border_width(GTK_CONTAINER(window), 24);
    g_signal_connect(window, "destroy", G_CALLBACK(gtk_main_quit), NULL);

    GtkWidget *button = gtk_button_new_with_label("Presioneme");
    g_signal_connect(button, "clicked", G_CALLBACK(on_button_clicked), window);

    gtk_container_add(GTK_CONTAINER(window), button);
    gtk_widget_show_all(window);

    gtk_main();
    return 0;
}
