package com.example.httpclient;

import com.formdev.flatlaf.FlatDarculaLaf;

import javax.swing.*;
public class App {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(App::createAndShowUI);
    }

    private static void createAndShowUI() {
        FlatDarculaLaf.setup();

        JFrame frame = new JFrame("HTTP Client Viewer");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setContentPane(new HttpClientPanel());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
