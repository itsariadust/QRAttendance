/*
QR Attendance Scanning System
Copyright (c) 2025 itsariadust
 */

package com.itsariadust.qrattendance;

// Dotenv for secure variable storage
import io.github.cdimascio.dotenv.Dotenv;

// JDBI for database
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;
import org.jdbi.v3.sqlobject.*;
import org.jdbi.v3.core.Handle;

// OpenCV libraries
import org.opencv.core.*;
import org.opencv.videoio.Videoio;
import org.opencv.videoio.VideoCapture;
import org.opencv.objdetect.QRCodeDetector;

// Java Swing libraries
import javax.imageio.ImageIO;
import javax.swing.*;

// Misc
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class QRAttendance {
    static Dotenv dotenv = Dotenv.configure()
            .directory("src/main/resources/.env")
            .load();

    // Set up JDBI connection
    static String dbUrl = dotenv.get("DB_URL");
    static String dbUser = dotenv.get("DB_USERNAME");
    static String dbPass = dotenv.get("DB_PASSWORD");
    static Jdbi jdbi;

    // DAOs for better table CRUD
    static StudentDao studentDao;
    static AttendanceDao attendanceDao;
    AttendanceTableModel attendanceTableModel;

    private VideoCapture camera;
    private static int cameraID;

    private boolean running;
    private boolean paused;

    private String studentNo;

    private SystemUI ui;

    // Load appropriate library based on OS
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

    public QRAttendance() {
        this.attendanceTableModel = new AttendanceTableModel();
    }

    public static void main(String[] args) {
        QRAttendance system = new QRAttendance();
        jdbi = Jdbi.create(dbUrl, dbUser, dbPass);
        jdbi.installPlugin(new SqlObjectPlugin());
        jdbi.registerRowMapper(BeanMapper.factory(Students.class));
        jdbi.registerRowMapper(BeanMapper.factory(Attendance.class));
        studentDao = jdbi.onDemand(StudentDao.class);
        attendanceDao = jdbi.onDemand(AttendanceDao.class);
        SwingUtilities.invokeLater(() -> {
            SystemUI ui = new SystemUI(system, system.attendanceTableModel);
            system.setUI(ui);
            ui.setVisible(true);
        });
        system.startPolling();
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
                    studentNo = detectQRCode(frame, qrDetector);
                    if (!studentNo.isEmpty()) {
                        if (checkStudent(studentDao, studentNo).isEmpty()) {
                            ui.invalidStudentDialog();
                            paused = true;
                            continue;
                        }
                        createEntry(attendanceDao, studentNo);
                        paused = true;
                    }
                    ui.updateLabel(frame);
                } else {
                    try {
                        ArrayList<String> studentInfo = getStudentInfo(studentDao, attendanceDao, studentNo);
                        ui.displayStudentInfo(studentInfo);
                        ui.displayImage(getStudentImage(studentDao, studentInfo));
                        Thread.sleep(3000);
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

    private Optional<Students> checkStudent(StudentDao dao, String studentNo) {
		return dao.findStudent(studentNo);
    }

    private ArrayList<String> getStudentInfo(StudentDao studentDao, AttendanceDao attendanceDao, String studentNo) {
        ArrayList<String> studentInfo = new ArrayList<>();
        Optional<Students> getStudent = studentDao.findStudent(studentNo);
        Optional<Attendance> attendanceRecord = checkAttendanceRecord(attendanceDao, studentNo);

        Attendance attendance = attendanceRecord.get();
        populateInfo(studentInfo, attendance, getStudent);
        return studentInfo;
    }

    private Optional<Attendance> checkAttendanceRecord(AttendanceDao dao, String studentNo) {
        return dao.findLatestRecord(studentNo);
    }

    private void createEntry(AttendanceDao dao, String studentNo) {
        Optional<Attendance> attendanceRecord = checkAttendanceRecord(dao, studentNo);
        if (attendanceRecord.isEmpty()) {
            dao.insert(studentNo, LocalDateTime.now(), "LOGGED IN");
            return;
        }

        Attendance attendance = attendanceRecord.get();

        if ("LOGGED IN".equals(attendance.getStatus())) {
            dao.insert(studentNo, LocalDateTime.now(), "LOGGED OUT");
            return;
        }

        dao.insert(studentNo, LocalDateTime.now(), "LOGGED IN");
    }

    public void startPolling() {
        Timer timer = new Timer(2000, e -> {
            try (Handle handle = jdbi.open()) {
                List<Attendance> records = fetchAttendanceRecords(handle);
                attendanceTableModel.setData(records); // UI update
            } catch (Exception ex) {
                ex.printStackTrace(); // Log appropriately
            }
        });
        timer.start();
    }

    public List<Attendance> fetchAttendanceRecords(Handle handle) {
        return handle.createQuery("SELECT * FROM attendance ORDER BY Timestamp DESC")
                .map((rs, ctx) -> {
                    Attendance record = new Attendance();
                    record.setAttendanceId(rs.getLong("AttendanceID"));
                    record.setStudentNo(rs.getLong("StudentNo"));
                    record.setTimestamp(rs.getTimestamp("Timestamp"));
                    record.setStatus(rs.getString("Status"));
                    return record;
                })
                .list();
    }

    ArrayList<String> getAttendanceInfo(AttendanceDao attendanceDao, StudentDao studentDao, String idValue) {
        ArrayList<String> attendanceInfo = new ArrayList<>();
        Optional<Attendance> attendanceRecord = attendanceDao.findSpecificRecord(idValue);
        Attendance attendance = attendanceRecord.get();

        Optional<Students> getStudent = studentDao.findStudent(String.valueOf(attendance.getStudentNo()));
        populateInfo(attendanceInfo, attendance, getStudent);
        attendanceInfo.add(attendance.getTimestamp().toString());
        return attendanceInfo;
    }

    private void populateInfo(ArrayList<String> attendanceInfo, Attendance attendance, Optional<Students> getStudent) {
        Students student = getStudent.get();

        attendanceInfo.add(Long.toString(student.getStudentNo()));
        attendanceInfo.add(student.getFirstName() + " " + student.getMiddleName() + " " + student.getLastName());
        attendanceInfo.add(student.getProgramId());
        attendanceInfo.add(student.getYearLevel());
        attendanceInfo.add(attendance.getStatus());
    }

    public ImageIcon getStudentImage(StudentDao studentDao, ArrayList<String> studentInfo) {
        try {
            if (studentInfo == null || studentInfo.isEmpty()) {
                return null;
            }

            byte[] studentImg = studentDao.findPicture(studentInfo.getFirst());

            if (studentImg == null || studentImg.length == 0) {
                return null;
            }

            InputStream is = new ByteArrayInputStream(studentImg);
            BufferedImage img = ImageIO.read(is);
            Image scaledImg = img.getScaledInstance(1000, 1000, Image.SCALE_SMOOTH);
            if (scaledImg == null) {
                System.out.println("ImageIO.read returned null – image data may be corrupted or invalid format.");
                return null;
            }

            return new ImageIcon(scaledImg);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
