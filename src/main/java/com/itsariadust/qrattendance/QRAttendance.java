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
    private String studentNumber;
    private String studentName;
    private String studentProgram;
    private String studentYearLevel;
    private String studentAttendanceStatus;

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
                    String studentNo = detectQRCode(frame, qrDetector);
                    if (!studentNo.isEmpty()) {
                        Boolean isStudent = checkStudent(studentNo);
                        if (!isStudent) {
                            ui.invalidStudentDialog();
                            paused = true;
                            continue;
                        }
                        createEntry(studentNo);
                        getInfo(studentNo);
                        ui.displayStudentInfo(studentNumber, studentName,
                                studentProgram, studentYearLevel,
                                studentAttendanceStatus);
                        paused = true;
                    }
                    ui.updateLabel(frame);
                } else {
                    try {
                        Thread.sleep(1000);
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
                    SELECT * FROM attendance
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
            createRecord(studentNo, "LOGGED IN");
            return;
        }

        Attendance attendance = attendanceRecord.get();

        if ("LOGGED IN".equals(attendance.getStatus())) {
            createRecord(studentNo, "LOGGED OUT");
            return;
        }

        createRecord(studentNo, "LOGGED IN");
    }

    private void getInfo(String studentNo) {
        Optional<Students> studentRecord = jdbi.withHandle(handle ->
            handle.createQuery("SELECT * FROM students WHERE StudentNo = :studentNo")
                    .bind("studentNo", studentNo)
                    .mapToBean(Students.class)
                    .findFirst()
            );
        Optional<Attendance> attendanceRecord = checkAttendanceRecord(studentNo);

        Attendance attendance = attendanceRecord.get();
        Students student = studentRecord.get();

        studentNumber = Long.toString(student.getStudentNo());
        studentName = student.getFirstName() + " " + student.getMiddleName() + " " + student.getLastName();
        studentProgram = student.getProgramId();
        studentYearLevel = student.getYearLevel();
        studentAttendanceStatus = attendance.getStatus();
    }

    private void createRecord(String studentNo, String status) {
        jdbi.useHandle(handle ->
                handle.createUpdate("""
                            INSERT INTO attendance (StudentNo, Timestamp, Status)
                            VALUES(
                                :studentNo,
                                NOW(),
                                :status
                            )
                        """)
                        .bind("studentNo", studentNo)
                        .bind("status", status)
                        .execute()
        );
    }
}
