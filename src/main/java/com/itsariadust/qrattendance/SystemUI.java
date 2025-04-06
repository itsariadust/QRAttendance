package com.itsariadust.qrattendance;

import org.opencv.core.Mat;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.text.*;
import java.util.Calendar;

public class SystemUI extends JFrame {
    private static QRAttendance qrAttendance;
    // Component variables
    private JLabel imageLabel;
    private JLabel label;
    private JLabel timeLabel;
    private JLabel dayLabel;
    private JLabel dateLabel;
    private JTextField[] textFields;

    // Variables for dates
    private SimpleDateFormat timeFormat;
    private SimpleDateFormat dayFormat;
    private SimpleDateFormat dateFormat;
    private String time;
    private String day;
    private String date;

    public SystemUI(QRAttendance qrAttendance) {
        this.qrAttendance = qrAttendance;
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
            textFields[i] = new JTextField(20);
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SystemUI ui = new SystemUI(qrAttendance);
            ui.setVisible(true);
        });
    }
}