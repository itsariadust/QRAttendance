/*
QR Attendance Scanning System
Copyright (c) 2025 itsariadust
 */

package com.itsariadust.qrattendance;

// OpenCV libraries
import org.opencv.core.*;
import org.opencv.videoio.Videoio;
import org.opencv.videoio.VideoCapture;
import org.opencv.objdetect.QRCodeDetector;

// Java Swing libraries
import javax.swing.*;

public class QRAttendance {
    private VideoCapture camera;
    private boolean running;
    private boolean paused;
    private SystemUI ui;

    static {
        System.load("/usr/lib/java/libopencv_java4100.so");
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            System.load("C:\\opencv\\build\\java\\x64\\opencv_java4100.dll");
        } else {
            System.load("/usr/lib/java/libopencv_java4100.so");
        }
    }

    public void setUI(SystemUI ui) {
        this.ui = ui;
        startCamera();
    }

    public static void main(String[] args) {
        QRAttendance system = new QRAttendance();
        SwingUtilities.invokeLater(() -> {
            SystemUI ui = new SystemUI(system);
            system.setUI(ui); // Now UI is set
            ui.setVisible(true);
        });
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
                    updateFeed(frame);
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
//        if (!decodedText.isEmpty()) {
//            Imgproc.putText(frame, decodedText, new org.opencv.core.Point(10, 30),
//                    Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 255, 0), 2);
//        }
        return decodedText;
    }

    private void updateFeed(Mat frame) {
        ui.updateLabel(frame);
    }
}
