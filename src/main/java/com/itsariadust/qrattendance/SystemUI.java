package com.itsariadust.qrattendance;

import org.opencv.core.Mat;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.*;
import java.util.ArrayList;
import java.util.Calendar;

import static com.itsariadust.qrattendance.QRAttendance.studentDao;

public class SystemUI extends JFrame {
    private static QRAttendance qrAttendance;
    private static AttendanceTableModel attendanceTableModel;
    private LogWindow logWindow;

    // Component variables
    private JLabel imageLabel;
    private JLabel label;
    private JLabel timeLabel;
    private JLabel dayLabel;
    private JLabel dateLabel;
    private JTextField[] textFields;
    private JButton logButton;
    private JTable logTable;
    private JLabel studentImg;

    // Variables for dates
    private SimpleDateFormat timeFormat;
    private SimpleDateFormat dayFormat;
    private SimpleDateFormat dateFormat;
    private String time;
    private String day;
    private String date;

    Font font = new Font("Arial", Font.PLAIN, 16);

    public SystemUI(QRAttendance qrAttendance, AttendanceTableModel model) {
        this.qrAttendance = qrAttendance;
        this.attendanceTableModel = model;
        setTitle("QR Attendance System");
        setSize(1280, 960);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.BOTH;

        // Left side: Camera feed
        JPanel cameraPanel = new JPanel();
        cameraPanel.setBackground(Color.BLACK);
        cameraPanel.setPreferredSize(new Dimension(600, 600));
        cameraPanel.setBorder(BorderFactory.createTitledBorder("Camera Feed"));

        imageLabel = new JLabel();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.weightx = 0.5;
        gbc.weighty = 1.0;
        add(cameraPanel, gbc);
        cameraPanel.add(imageLabel);

        // Right top: Student info panel
        JPanel studentInfoPanel = new JPanel(new GridBagLayout());
        studentInfoPanel.setBorder(BorderFactory.createTitledBorder("Student Info"));

        GridBagConstraints infoGbc = new GridBagConstraints();
        infoGbc.insets = new Insets(5, 5, 5, 5);
        infoGbc.fill = GridBagConstraints.HORIZONTAL;
        infoGbc.gridx = 0;
        infoGbc.gridy = 0;

        // Labels and TextFields
        String[] labels = {"Student No:", "Name:", "Program:", "Year Level:", "Status:"};
        textFields = new JTextField[labels.length];

        for (int i = 0; i < labels.length; i++) {
            label = new JLabel(labels[i]);
            label.setFont(font);
            textFields[i] = new JTextField(20);
            textFields[i].setFont(font);
            textFields[i].setEditable(false);
            textFields[i].setFocusable(false);

            infoGbc.gridx = 0; // Label on the left
            infoGbc.weightx = 0.3;
            studentInfoPanel.add(label, infoGbc);

            infoGbc.gridx = 1; // Text field on the right
            infoGbc.weightx = 0.7;
            studentInfoPanel.add(textFields[i], infoGbc);

            infoGbc.gridy++; // Move to next row
        }

        logButton = new JButton("Logs");
        logButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // If secondFrame is null or not showing, create or show it
                if (logWindow == null || !logWindow.isDisplayable()) {
                    logWindow = new LogWindow();
                } else {
                    // Bring it to front if it's already open
                    logWindow.toFront();
                    logWindow.requestFocus();
                }

                // Show the frame
                logWindow.setVisible(true);
            }
        });
        studentInfoPanel.add(logButton, infoGbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = 1;
        gbc.weightx = 0.5;
        gbc.weighty = 0.3;
        add(studentInfoPanel, gbc);

        // Right bottom: Clock
        JPanel clockPanel = new JPanel(new BorderLayout());
        clockPanel.setBorder(BorderFactory.createTitledBorder("Clock"));
        timeFormat = new SimpleDateFormat("kk:mm:ss");
        dayFormat = new SimpleDateFormat("EEEE");
        dateFormat = new SimpleDateFormat("MMMMM dd, yyyy");

        timeLabel = new JLabel("", SwingConstants.CENTER);  // Center aligned
        timeLabel.setFont(new Font("Verdana", Font.PLAIN, 100));

        dayLabel = new JLabel("", SwingConstants.CENTER);
        dayLabel.setFont(new Font("Verdana", Font.PLAIN, 50));

        dateLabel = new JLabel("", SwingConstants.CENTER);
        dateLabel.setFont(new Font("Verdana", Font.PLAIN, 50));

        clockPanel.add(timeLabel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new GridLayout(2, 1));
        bottomPanel.add(dayLabel);
        bottomPanel.add(dateLabel);
        clockPanel.add(bottomPanel, BorderLayout.SOUTH);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weighty = 0.7;
        add(clockPanel, gbc);

        setTime();
    }

    public void updateLabel(Mat frame) {
        BufferedImage image = matToBufferedImage(frame);
        if (image != null) {
            imageLabel.setIcon(new ImageIcon(image));
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

    public void displayImage(ImageIcon image) {
        imageLabel.setIcon(image);
    }

    public void setTime() {
        // Update time immediately
        updateClock();

        // Set up a timer to update the clock every second
        Timer timer = new Timer(1000, e -> updateClock());
        timer.start();
    }

    private void updateClock() {
        time = timeFormat.format(Calendar.getInstance().getTime());
        timeLabel.setText(time);

        day = dayFormat.format(Calendar.getInstance().getTime());
        dayLabel.setText(day);

        date = dateFormat.format(Calendar.getInstance().getTime());
        dateLabel.setText(date);
    }

    public void displayStudentInfo(ArrayList<String> studentInfo) {
        textFields[0].setText(studentInfo.get(0)); // Student No.
        textFields[1].setText(studentInfo.get(1)); // Name
        textFields[2].setText(studentInfo.get(2)); // Program
        textFields[3].setText(studentInfo.get(3)); // Year Level
        textFields[4].setText(studentInfo.get(4)); // Status
    }

    public void invalidStudentDialog() {
        JOptionPane.showMessageDialog(this.rootPane, "Invalid student. Please try again.", "Warning", JOptionPane.WARNING_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SystemUI ui = new SystemUI(qrAttendance, attendanceTableModel);
            ui.setVisible(true);
        });
    }

    class LogWindow extends JFrame {
        public LogWindow() {
            setTitle("AttendanceLog");
            setSize(800, 600);
            setLocationRelativeTo(null);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE); // Allows closing this frame without exiting the app
            setLayout(new GridBagLayout());

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.fill = GridBagConstraints.BOTH;

            // top: Attendance info panel
            JPanel attendanceInfo = new JPanel(new GridBagLayout());
            attendanceInfo.setBorder(BorderFactory.createTitledBorder("Attendance Info"));

            GridBagConstraints infoGbc = new GridBagConstraints();
            infoGbc.insets = new Insets(5, 5, 5, 5);
            infoGbc.fill = GridBagConstraints.HORIZONTAL;
            infoGbc.gridx = 0;
            infoGbc.gridy = 0;

            JPanel attendanceInfoFields = new JPanel(new GridBagLayout());
            GridBagConstraints infoFieldsGbc = new GridBagConstraints();
            infoFieldsGbc.insets = new Insets(5, 5, 5, 5);
            infoFieldsGbc.fill = GridBagConstraints.HORIZONTAL;
            infoFieldsGbc.gridx = 0;
            infoFieldsGbc.gridy = 0;

            // Labels and TextFields
            String[] labels = {"Student No:", "Name:", "Program:", "Year Level:", "Status:", "Timestamp: "};
            textFields = new JTextField[labels.length];

            for (int i = 0; i < labels.length; i++) {
                label = new JLabel(labels[i]);
                label.setFont(font);
                textFields[i] = new JTextField(20);
                textFields[i].setFont(font);
                textFields[i].setEditable(false);
                textFields[i].setFocusable(false);

                infoFieldsGbc.gridx = 1; // Label on the left
                infoFieldsGbc.weightx = 0.3;
                attendanceInfoFields.add(label, infoFieldsGbc);

                infoFieldsGbc.gridx = 2; // Text field on the right
                infoFieldsGbc.weightx = 0.5;
                attendanceInfoFields.add(textFields[i], infoFieldsGbc);

                infoFieldsGbc.gridy++; // Move to next row
            }
            infoGbc.gridx = 1;
            attendanceInfo.add(attendanceInfoFields, infoGbc);

            // Student Image
            studentImg = new JLabel();
            infoGbc.gridx = 0;
            attendanceInfo.add(studentImg, infoGbc);

            gbc.gridx = 1;
            gbc.gridy = 0;
            gbc.gridheight = 1;
            gbc.weightx = 0.5;
            gbc.weighty = 0.3;
            add(attendanceInfo, gbc);

            // bottom: Log
            JPanel logPanel = new JPanel(new BorderLayout());
            logPanel.setBorder(BorderFactory.createTitledBorder("Log"));

            logTable = new JTable(attendanceTableModel) {
                public boolean isCellEditable(int row, int column) {
                    return false;
                };
            };
            logTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            addTableSelectionListener(logTable);
            JScrollPane sp = new JScrollPane(logTable);
            logPanel.add(sp, BorderLayout.CENTER);

            gbc.gridx = 1;
            gbc.gridy = 1;
            gbc.weighty = 0.7;
            add(logPanel, gbc);
        }

        private void addTableSelectionListener(JTable table) {
            table.getSelectionModel().addListSelectionListener(event -> {
                if (!event.getValueIsAdjusting()) {
                    int selectedRow = table.getSelectedRow();
                    if (selectedRow != -1) {
                        int modelRow = table.convertRowIndexToModel(selectedRow);
                        String idValue = table.getModel().getValueAt(modelRow, 0).toString();
                        ArrayList<String> record = qrAttendance.getAttendanceInfo(QRAttendance.attendanceDao,
                                studentDao, idValue);
                        byte[] studentImgByte = studentDao.findPicture(record.get(0));
                        InputStream is = new ByteArrayInputStream(studentImgByte);
                        BufferedImage img = null;
                        try {
                            img = ImageIO.read(is);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        Image scaledImg = img.getScaledInstance(300, 300, Image.SCALE_SMOOTH);
                        if (scaledImg == null) {
                            System.out.println("ImageIO.read returned null – image data may be corrupted or invalid format.");
                            return;
                        }
                        studentImg.setIcon(new ImageIcon(scaledImg));
                        textFields[0].setText(record.get(0)); // Student No.
                        textFields[1].setText(record.get(1)); // Name
                        textFields[2].setText(record.get(2)); // Program
                        textFields[3].setText(record.get(3)); // Year Level
                        textFields[4].setText(record.get(4)); // Status
                        textFields[5].setText(record.get(5)); // Timestamp
                    }
                }
            });
        }
    }
}