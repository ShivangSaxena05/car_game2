import javax.swing.*;
import java.util.ArrayList;
import java.util.Random;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import org.opencv.core.*;
import org.opencv.videoio.VideoCapture;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.MatOfPoint;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.List;

public class Car_Game extends JPanel implements ActionListener, KeyListener {
    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    int width = 400, height = 640;
    Image bgimg, carimg, encarimg;
    int carX = width / 2 - 15, carY = 500;
    int carWidth = 34, carHeight = 60;
    boolean gameover = false;
    double score = 0;
    JFrame cameraFrame;
    JLabel cameraLabel;

    class Car {
        int x = carX, y = carY, width = carWidth, height = carHeight;
        Image img;
        Car(Image img) { this.img = img; }
    }

    class EnCar {
        int x, y, width = 34, height = 60;
        Image img;
        boolean passed = false;
        EnCar(Image img) {
            this.img = img;
            this.x = lanePositions[new Random().nextInt(lanePositions.length)];
            this.y = 10;
        }
    }

    Car car;
    ArrayList<EnCar> encars = new ArrayList<>();
    Random rndm = new Random();
    Timer gmloop, plTimer;
    VideoCapture capture;
    Mat frame, grayFrame;
    int[] lanePositions = {60, 140, 220, 300};

    Car_Game() {
        setPreferredSize(new Dimension(width, height));
        setFocusable(true);
        addKeyListener(this);

        bgimg = loadImage("assets/road.png");
        carimg = loadImage("assets/play_car.png");
        encarimg = loadImage("assets/car.png");

        car = new Car(carimg);

        plTimer = new Timer(1500, e -> placeCars());
        plTimer.start();
        gmloop = new Timer(10, this);
        gmloop.start();

        capture = new VideoCapture(0);
        if (!capture.isOpened()) {
            System.out.println("Error: Camera not found.");
            capture = null;
        } else {
            frame = new Mat();
            grayFrame = new Mat();
            setupCameraFrame();
        }
    }

    private void setupCameraFrame() {
        cameraFrame = new JFrame("Camera Feed");
        cameraLabel = new JLabel();
        cameraFrame.add(cameraLabel);
        cameraFrame.setSize(640, 480);
        cameraFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        cameraFrame.setVisible(true);
    }

    private Image loadImage(String path) {
        ImageIcon icon = new ImageIcon(getClass().getResource(path));
        return icon.getImage() != null ? icon.getImage() : new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    }

    public void placeCars() {
        encars.add(new EnCar(encarimg));
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    public void draw(Graphics g) {
        g.drawImage(bgimg, 0, 0, width, height, null);
        g.drawImage(car.img, carX, carY, carWidth, carHeight, null);
        for (EnCar encar : encars) {
            g.drawImage(encar.img, encar.x, encar.y, encar.width, encar.height, null);
        }
        g.setColor(Color.white);
        g.setFont(new Font("Arial", Font.PLAIN, 32));
        g.drawString(gameover ? "Game Over: " + score : String.valueOf(score), 10, 35);
    }

    public void leftmove() {
        if (!gameover && carX > 40) carX -= 5;
    }

    public void rightmove() {
        if (!gameover && carX < width - (carWidth + 40)) carX += 5;
    }

    public void movedown() {
        for (EnCar encar : new ArrayList<>(encars)) {
            encar.y += 2;
            if (collision(car, encar)) {
                gameover = true;
                break;
            }
            if (!encar.passed && encar.y + encar.height > carY + carHeight) {
                encar.passed = true;
                score += 1;
            }
        }
    }

    public boolean collision(Car a, EnCar b) {
        return (a.y < b.y + b.height && a.y + a.height > b.y && a.x < b.x + b.width && a.x + a.width > b.x);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameover) {
            movedown();
            if (capture != null && capture.isOpened()) {
                capture.read(frame);
                if (!frame.empty()) {
                    Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
                    List<MatOfPoint> contours = new ArrayList<>();
                    Mat hierarchy = new Mat();
                    Imgproc.findContours(grayFrame, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
                    int contourCount = (int) contours.stream().filter(c -> Imgproc.contourArea(c) > 1000).count();
                    if (contourCount == 1) leftmove();
                    else if (contourCount > 4) rightmove();
                    ImageIcon icon = new ImageIcon(Mat2BufferedImage(frame));
                    cameraLabel.setIcon(icon);
                }
            }
            repaint();
        } else {
            plTimer.stop();
            gmloop.stop();
            JOptionPane.showMessageDialog(null, "Game Over");
        }
    }

    private BufferedImage Mat2BufferedImage(Mat mat) {
        int type = BufferedImage.TYPE_3BYTE_BGR;
        if (mat.channels() == 1) {
            type = BufferedImage.TYPE_BYTE_GRAY;
        }
        BufferedImage image = new BufferedImage(mat.width(), mat.height(), type);
        mat.get(0, 0, ((DataBufferByte) image.getRaster().getDataBuffer()).getData());
        return image;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) rightmove();
        else if (e.getKeyCode() == KeyEvent.VK_LEFT) leftmove();
    }

    @Override
    public void keyTyped(KeyEvent e) {}
    @Override
    public void keyReleased(KeyEvent e) {}
}