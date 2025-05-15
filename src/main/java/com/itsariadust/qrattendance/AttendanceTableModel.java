package com.itsariadust.qrattendance;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class AttendanceTableModel extends AbstractTableModel {
    private final String[] columns = { "Record ID", "Student No.", "Timestamp", "Status" };
    private List<Attendance> data = new ArrayList<>();

    public void setData(List<Attendance> newData) {
        this.data = newData;
        fireTableDataChanged(); // Notify JTable to refresh
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public String getColumnName(int col) {
        return columns[col];
    }

    @Override
    public Object getValueAt(int row, int col) {
        Attendance record = data.get(row);
        return switch (col) {
            case 0 -> record.getAttendanceId();
            case 1 -> record.getStudentNo();
            case 2 -> record.getTimestamp();
            case 3 -> record.getStatus();
            default -> null;
        };
    }
}

