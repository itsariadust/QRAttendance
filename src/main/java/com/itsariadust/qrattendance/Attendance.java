package com.itsariadust.qrattendance;

public class Attendance {

  private long attendanceId;
  private long studentNo;
  private java.sql.Timestamp timestamp;
  private String status;

  public Attendance() {}

  public long getAttendanceId() {
    return attendanceId;
  }

  public void setAttendanceId(long attendanceId) {
    this.attendanceId = attendanceId;
  }


  public long getStudentNo() {
    return studentNo;
  }

  public void setStudentNo(long studentNo) {
    this.studentNo = studentNo;
  }


  public java.sql.Timestamp getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(java.sql.Timestamp timestamp) {
    this.timestamp = timestamp;
  }


  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

}
