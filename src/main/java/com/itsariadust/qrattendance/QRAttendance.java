/*
QR Attendance Scanning System
Copyright (c) 2025 itsariadust
 */

package com.itsariadust.qrattendance;

// Dotenv for secure variable storage
import io.github.cdimascio.dotenv.Dotenv;

// JDBI for database
import org.jdbi.v3.core.Jdbi;

// OpenCV libraries
import org.opencv.core.*;
import org.opencv.videoio.Videoio;
import org.opencv.videoio.VideoCapture;
import org.opencv.objdetect.QRCodeDetector;

// Java Swing libraries
import javax.swing.*;

// Misc
import java.util.Optional;

public class QRAttendance {
    static Dotenv dotenv = Dotenv.load();
    static String dbUrl = dotenv.get("DB_URL");
    static String dbUser = dotenv.get("DB_USERNAME");
    static String dbPass = dotenv.get("DB_PASSWORD");
    static Jdbi jdbi = Jdbi.create(dbUrl, dbUser, dbPass);

    private VideoCapture camera;
    private boolean running;
    private boolean paused;
    private SystemUI ui;
    private static int cameraID;

    static {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            System.load("C:\\opencv\\build\\java\\x64\\opencv_java4100.dll");
            cameraID = 0;
        } else {
            System.load("/usr/lib/java/libopencv_java4100.so");
            cameraID = 1;
        }
    }

    public static void main(String[] args) {
        QRAttendance system = new QRAttendance();
        SwingUtilities.invokeLater(() -> {
            SystemUI ui = new SystemUI(system);
            system.setUI(ui);
            ui.setVisible(true);
        });
    }

    public void setUI(SystemUI ui) {
        this.ui = ui;
        startCamera();
    }

    private void startCamera() {
        camera = new VideoCapture(cameraID);
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
                    String studentNo = detectQRCode(frame, qrDetector);
                    if (!studentNo.isEmpty()) {
                        System.out.println("QR Code Detected: " + studentNo);
                        Boolean isStudent = checkStudent(studentNo);
                        if (!isStudent) {
                            System.out.println("Doesn't exist");
                            return;
                        }
                        System.out.println("Exists");
                        createEntry(studentNo);
                        paused = true;
                    }
                    ui.updateLabel(frame);
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
		return qrDetector.detectAndDecode(frame, points);
    }

    private Boolean checkStudent(String studentNo) {
        return jdbi.withHandle(handle ->
            handle.createQuery("SELECT COUNT(*) FROM students WHERE StudentNo = :studentNo")
                    .bind("studentNo", studentNo)
                    .mapTo(Integer.class)
                    .one() > 0
        );
    }

    private Optional<Attendance> checkAttendanceRecord(String studentNo) {
        return jdbi.withHandle(handle ->
            handle.createQuery("""
                    SELECT * FROM Attendance
                    WHERE StudentNo = :studentNo
                    ORDER BY Timestamp DESC
                    LIMIT 1
                    """)
                    .bind("studentNo", studentNo)
                    .mapToBean(Attendance.class)
                    .findFirst()
        );
    }

    private void createEntry(String studentNo) {
        Optional<Attendance> attendanceRecord = checkAttendanceRecord(studentNo);
        if (attendanceRecord.isEmpty()) {
            createLogInRecord(studentNo);
            return;
        }

        Attendance attendance = attendanceRecord.get();

        if ("LOGGED IN".equals(attendance.getStatus())) {
            createLogOutRecord(studentNo);
            return;
        }

        createLogInRecord(studentNo);
    }

    private void createLogInRecord(String studentNo) {
        jdbi.useHandle(handle ->
                handle.createUpdate("""
                            INSERT INTO ATTENDANCE (StudentNo, Timestamp, Status)
                            VALUES(
                                :studentNo,
                                NOW(),
                                :status
                            )
                        """)
                        .bind("studentNo", studentNo)
                        .bind("status", "LOGGED IN")
                        .execute()
        );
    }

    private void createLogOutRecord(String studentNo) {
        jdbi.useHandle(handle ->
                handle.createUpdate("""
                            INSERT INTO ATTENDANCE (StudentNo, Timestamp, Status)
                            VALUES(
                                :studentNo,
                                NOW(),
                                :status
                            )
                        """)
                        .bind("studentNo", studentNo)
                        .bind("status", "LOGGED OUT")
                        .execute()
        );
    }
}
