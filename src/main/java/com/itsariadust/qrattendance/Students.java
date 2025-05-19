package com.itsariadust.qrattendance;

public class Students {

  private long studentNo;
  private String firstName;
  private String middleName;
  private String lastName;
  private String programId;
  private String yearLevel;
  private byte[] picture;


  public long getStudentNo() {
    return studentNo;
  }

  public void setStudentNo(long studentNo) {
    this.studentNo = studentNo;
  }


  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }


  public String getMiddleName() {
    return middleName;
  }

  public void setMiddleName(String middleName) {
    this.middleName = middleName;
  }


  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }


  public String getProgramId() {
    return programId;
  }

  public void setProgramId(String programId) {
    this.programId = programId;
  }


  public String getYearLevel() {
    return yearLevel;
  }

  public void setYearLevel(String yearLevel) {
    this.yearLevel = yearLevel;
  }


  public byte[] getPicture() {
    return picture;
  }

  public void setPicture(byte[] picture) {
    this.picture = picture;
  }

}
