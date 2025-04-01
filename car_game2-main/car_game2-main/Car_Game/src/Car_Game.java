import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.*;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

public class Car_Game extends JPanel implements ActionListener, KeyListener {
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    private static final int WIDTH = 400, HEIGHT = 640;
    private static final int CAR_WIDTH = 34, CAR_HEIGHT = 60;
    private static final int[] LANE_POSITIONS = {60, 140, 220, 300};
    private static final long GESTURE_COOLDOWN = 500;

    private Image bgImg, carImg, enemyCarImg;
    private int carX = WIDTH / 2 - CAR_WIDTH / 2, carY = 500;
    private boolean gameOver = false;
    private double score = 0;
    private final Car playerCar;
    private final ArrayList<EnemyCar> enemyCars = new ArrayList<>();
    private final Random random = new Random();
    private final Timer gameLoop, enemySpawnTimer;

    private VideoCapture capture;
    private Mat frame;
    private JFrame cameraFrame;
    private JLabel cameraLabel;
    private long lastGestureTime = 0;

    private class Car {
        int x, y;
        Image img;

        Car(Image img, int x, int y) {
            this.img = img;
            this.x = x;
            this.y = y;
        }
    }

    private class EnemyCar {
        int x, y;
        Image img;

        EnemyCar(Image img) {
            this.img = img;
            this.x = LANE_POSITIONS[random.nextInt(LANE_POSITIONS.length)];
            this.y = -CAR_HEIGHT;
        }
    }

    public Car_Game() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        addKeyListener(this);

        bgImg = loadImage("assets/road.png");
        carImg = loadImage("assets/play_car.png");
        enemyCarImg = loadImage("assets/car.png");

        playerCar = new Car(carImg, carX, carY);

        enemySpawnTimer = new Timer(1500, e -> spawnEnemyCar());
        enemySpawnTimer.start();

        gameLoop = new Timer(10, this);
        gameLoop.start();

        initializeCamera();
    }

    private Image loadImage(String path) {
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource(path));
            return icon.getImage();
        } catch (Exception e) {
            System.err.println("Error loading image: " + path);
            return new BufferedImage(CAR_WIDTH, CAR_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        }
    }

    private void initializeCamera() {
        capture = new VideoCapture(0);
        if (!capture.isOpened()) {
            System.err.println("Camera initialization failed");
            return;
        }
        frame = new Mat();
        setupCameraWindow();
    }

    private void setupCameraWindow() {
        cameraFrame = new JFrame("Camera Feed");
        cameraLabel = new JLabel();
        cameraFrame.add(cameraLabel);
        cameraFrame.setSize(640, 480);
        cameraFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        cameraFrame.setVisible(true);
    }

    private void spawnEnemyCar() {
        if (!gameOver) {
            enemyCars.add(new EnemyCar(enemyCarImg));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        renderGame(g);
    }

    private void renderGame(Graphics g) {
        g.drawImage(bgImg, 0, 0, WIDTH, HEIGHT, null);
        g.drawImage(playerCar.img, playerCar.x, playerCar.y, CAR_WIDTH, CAR_HEIGHT, null);
        for (EnemyCar enemy : enemyCars) {
            g.drawImage(enemy.img, enemy.x, enemy.y, CAR_WIDTH, CAR_HEIGHT, null);
        }
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 32));
        g.drawString(gameOver ? "Game Over: " + (int) score : String.valueOf((int) score), 20, 40);
    }

    private void updateGame() {
        if (gameOver) {
            gameLoop.stop();
            enemySpawnTimer.stop();
            return;
        }

        for (EnemyCar enemy : new ArrayList<>(enemyCars)) {
            enemy.y += 2;
            if (enemy.y > HEIGHT) {
                enemyCars.remove(enemy);
                score++;
            }
        }

        processCameraInput();
    }

    private void processCameraInput() {
        if (capture == null || !capture.isOpened()) return;

        if (capture.read(frame) && !frame.empty()) {
            Core.flip(frame, frame, 1); // Mirror effect
            Mat ycrcbFrame = new Mat();
            Imgproc.cvtColor(frame, ycrcbFrame, Imgproc.COLOR_BGR2YCrCb);

            // Skin color range in YCrCb
            Scalar lowerSkin = new Scalar(0, 133, 77);
            Scalar upperSkin = new Scalar(255, 173, 127);
            Mat skinMask = new Mat();
            Core.inRange(ycrcbFrame, lowerSkin, upperSkin, skinMask);

            // Morphological operations to reduce noise
            Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5));
            Imgproc.morphologyEx(skinMask, skinMask, Imgproc.MORPH_OPEN, kernel);
            Imgproc.morphologyEx(skinMask, skinMask, Imgproc.MORPH_CLOSE, kernel);

            // Find contours
            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(skinMask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            MatOfPoint largestContour = null;
            double maxArea = 0;

            // Filter for the largest contour (hand)
            for (MatOfPoint contour : contours) {
                double area = Imgproc.contourArea(contour);
                if (area > 5000 && area > maxArea) {  // Ignore small noise
                    maxArea = area;
                    largestContour = contour;
                }
            }

            if (largestContour != null) {
                // Draw outline around the detected hand
                Imgproc.drawContours(frame, List.of(largestContour), -1, new Scalar(0, 255, 0), 3);

                // Get bounding box of hand
                Rect boundingBox = Imgproc.boundingRect(largestContour);
                int handCenterX = boundingBox.x + boundingBox.width / 2;

                // Move car based on hand position
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastGestureTime > GESTURE_COOLDOWN) {
                    if (handCenterX < frame.width() / 3) {
                        moveLeft();  // Move left if hand is in left 1/3
                    } else if (handCenterX > (frame.width() * 2) / 3) {
                        moveRight(); // Move right if hand is in right 1/3
                    }
                    lastGestureTime = currentTime;
                }
            }

            // Show the processed frame
            cameraLabel.setIcon(new ImageIcon(matToBufferedImage(frame)));
        }
    }


    private BufferedImage matToBufferedImage(Mat mat) {
        int type = mat.channels() == 1 ? BufferedImage.TYPE_BYTE_GRAY : BufferedImage.TYPE_3BYTE_BGR;
        BufferedImage image = new BufferedImage(mat.width(), mat.height(), type);
        mat.get(0, 0, ((DataBufferByte) image.getRaster().getDataBuffer()).getData());
        return image;
    }

    private void moveLeft() {
        if (playerCar.x > 40) playerCar.x -= 10;
    }

    private void moveRight() {
        if (playerCar.x < WIDTH - (CAR_WIDTH + 40)) playerCar.x += 10;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        updateGame();
        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT) moveLeft();
        else if (e.getKeyCode() == KeyEvent.VK_RIGHT) moveRight();
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {}
}
