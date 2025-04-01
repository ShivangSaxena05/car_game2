import javax.swing.*;
import org.opencv.core.Core;

public class App {
    public static void main(String[] args) {
        // Load OpenCV (if available)
        try {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Warning: OpenCV not found. Camera controls disabled.");
        }

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Car Game");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.setLocationRelativeTo(null);
            Car_Game carGame = new Car_Game();
            frame.add(carGame);
            frame.pack(); // Respect preferredSize from Car_Game
            frame.setVisible(true);

            // Request focus for key events
            carGame.requestFocus();
        });
    }
}