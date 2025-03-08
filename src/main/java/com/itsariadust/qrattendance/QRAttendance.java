/*
QR Attendance Scanning System
Copyright (c) 2025 itsariadust
 */

package com.itsariadust.qrattendance;

// OpenCV libraries
import org.opencv.core.*;
import org.opencv.videoio.Videoio;
import org.opencv.videoio.VideoCapture;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.QRCodeDetector;

// Java Swing libraries
import javax.swing.*;
import java.awt.image.BufferedImage;

public class QRAttendance {
    private VideoCapture camera;
    private JLabel imageLabel;
    private boolean running;
    private boolean paused;

    static {
        System.load("/usr/lib/java/libopencv_java4100.so");
    }

    public QRAttendance() {
        setupUI();
        startCamera();
    }

    public static void main(String[] args) {
        new QRAttendance();
    }

    private void setupUI() {
        JFrame frame = new JFrame("QR Scanner");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(640, 480);
        imageLabel = new JLabel();
        frame.add(imageLabel);
        frame.setVisible(true);
    }

    private void startCamera() {
        camera = new VideoCapture(1);
        if (!camera.isOpened()) {
            System.out.println("Error: Camera not found!");
            return;
        }

        camera.set(Videoio.CAP_PROP_FRAME_WIDTH, 1280);
        camera.set(Videoio.CAP_PROP_FRAME_HEIGHT, 720);

        running = true;
        paused = false;
        QRCodeDetector qrDetector = new QRCodeDetector();

        new Thread(() -> {
            Mat frame = new Mat();
            while (running) {
                if (!paused && camera.read(frame)) {
                    String qrText = detectQRCode(frame, qrDetector);
                    if (!qrText.isEmpty()) {
                        System.out.println("QR Code Detected: " + qrText);
                        paused = true;
                    }
                    updateUI(frame);
                } else {
                    try {
                        Thread.sleep(2000);
                        paused = false;
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            camera.release();
        }).start();
    }

    private String detectQRCode(Mat frame, QRCodeDetector qrDetector) {
        Mat points = new Mat();
        String decodedText = qrDetector.detectAndDecode(frame, points);
        if (!decodedText.isEmpty()) {
            Imgproc.putText(frame, decodedText, new org.opencv.core.Point(10, 30),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 255, 0), 2);
        }
        return decodedText;
    }

    private void updateUI(Mat frame) {
        BufferedImage image = matToBufferedImage(frame);
        if (image != null) {
            SwingUtilities.invokeLater(() -> imageLabel.setIcon(new ImageIcon(image)));
        }
    }

    private BufferedImage matToBufferedImage(Mat mat) {
        int width = mat.width(), height = mat.height(), channels = mat.channels();
        byte[] sourcePixels = new byte[width * height * channels];
        mat.get(0, 0, sourcePixels);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        image.getRaster().setDataElements(0, 0, width, height, sourcePixels);
        return image;
    }
}
