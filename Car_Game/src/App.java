import javax.swing.*;
import org.opencv.core.Core;

public class App {
    public static void main(String[] args) {
        // Load the OpenCV native library
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // Create the game window
        int gameWidth = 400;
        int gameHeight = 640;

        JFrame frame = new JFrame("Car Game");
        frame.setSize(gameWidth, gameHeight);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create the game panel
        Car_Game carGame = new Car_Game();
        frame.add(carGame);
        frame.pack(); // This will respect the preferred size set in Car_Game
        frame.setVisible(true); // Make the frame visible after adding the game panel

        // Request focus for key events
        carGame.requestFocus();
    }
}