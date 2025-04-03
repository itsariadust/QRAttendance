package com.itsariadust.qrattendance;

import org.opencv.core.Mat;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class SystemUI extends JFrame {
    private static QRAttendance qrAttendance;
    private JLabel imageLabel;

    public SystemUI(QRAttendance qrAttendance) {
        this.qrAttendance = qrAttendance;
        setTitle("QR Attendance System");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.BOTH;

        // Left side: Camera feed
        JPanel cameraPanel = new JPanel();
        cameraPanel.setBackground(Color.BLACK);
        cameraPanel.setPreferredSize(new Dimension(400, 600));
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
        JTextField[] textFields = new JTextField[labels.length];

        for (int i = 0; i < labels.length; i++) {
            JLabel label = new JLabel(labels[i]);
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

        // Right bottom: Recent log table
        String[] columnNames = {"Student ID", "Name", "Time", "Status"};
        JTable logTable = new JTable(new Object[][]{}, columnNames);
        JScrollPane scrollPane = new JScrollPane(logTable);
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("Recent Log"));
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weighty = 0.7;
        add(tablePanel, gbc);
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SystemUI ui = new SystemUI(qrAttendance);
            ui.setVisible(true);
        });
    }
}