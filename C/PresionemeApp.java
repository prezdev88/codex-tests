import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;

public class PresionemeApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(PresionemeApp::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Aplicación de Botón");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JButton button = new JButton("Presioneme");
        button.addActionListener(event ->
                JOptionPane.showMessageDialog(frame,
                        "¡Botón presionado!",
                        "Mensaje",
                        JOptionPane.INFORMATION_MESSAGE));

        frame.getContentPane().add(button, BorderLayout.CENTER);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
