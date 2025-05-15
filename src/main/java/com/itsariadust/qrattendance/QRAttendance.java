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
import javax.swing.*;

// Misc
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class QRAttendance {
    static Dotenv dotenv = Dotenv.configure()
            .directory("src/main/resources/.env")
            .load();
    static String dbUrl = dotenv.get("DB_URL");
    static String dbUser = dotenv.get("DB_USERNAME");
    static String dbPass = dotenv.get("DB_PASSWORD");
    static Jdbi jdbi;
    static StudentDao studentDao;
    static AttendanceDao attendanceDao;
    AttendanceTableModel attendanceTableModel;

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
                    String studentNo = detectQRCode(frame, qrDetector);
                    if (!studentNo.isEmpty()) {
                        if (checkStudent(studentDao, studentNo).isEmpty()) {
                            ui.invalidStudentDialog();
                            paused = true;
                            continue;
                        }
                        createEntry(attendanceDao, studentNo);
                        getStudentInfo(studentDao, attendanceDao, studentNo);
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

    private Optional<Students> checkStudent(StudentDao dao, String studentNo) {
		return dao.findStudent(studentNo);
    }

    private void getStudentInfo(StudentDao studentDao, AttendanceDao attendanceDao, String studentNo) {
        Optional<Students> getStudent = studentDao.findStudent(studentNo);
        Optional<Attendance> attendanceRecord = checkAttendanceRecord(attendanceDao, studentNo);

        Attendance attendance = attendanceRecord.get();
        Students student = getStudent.get();

        studentNumber = Long.toString(student.getStudentNo());
        studentName = student.getFirstName() + " " + student.getMiddleName() + " " + student.getLastName();
        studentProgram = student.getProgramId();
        studentYearLevel = student.getYearLevel();
        studentAttendanceStatus = attendance.getStatus();
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
        ArrayList<String> record = new ArrayList<>();
        Optional<Attendance> attendanceRecord = attendanceDao.findSpecificRecord(idValue);
        Attendance attendance = attendanceRecord.get();

        Optional<Students> getStudent = studentDao.findStudent(String.valueOf(attendance.getStudentNo()));
        Students student = getStudent.get();

        record.add(Long.toString(student.getStudentNo()));
        record.add(student.getFirstName() + " " + student.getMiddleName() + " " + student.getLastName());
        record.add(student.getProgramId());
        record.add(student.getYearLevel());
        record.add(attendance.getStatus());
        record.add(attendance.getTimestamp().toString());
        return record;
    }
}
