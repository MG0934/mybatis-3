package com.practice.bean;

import java.util.Date;

public class Student {

  private Integer studentId;

  private String name;

  private String phone;

  private String email;

  private Byte sex;

  private Byte locked;

  private Date gmtCreated;

  private Date gmtModified;

  public Integer getStudentId() {
    return studentId;
  }

  public void setStudentId(Integer studentId) {
    this.studentId = studentId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Byte getSex() {
    return sex;
  }

  public void setSex(Byte sex) {
    this.sex = sex;
  }

  public Byte getLocked() {
    return locked;
  }

  public void setLocked(Byte locked) {
    this.locked = locked;
  }

  public Date getGmtCreated() {
    return gmtCreated;
  }

  public void setGmtCreated(Date gmtCreated) {
    this.gmtCreated = gmtCreated;
  }

  public Date getGmtModified() {
    return gmtModified;
  }

  public void setGmtModified(Date gmtModified) {
    this.gmtModified = gmtModified;
  }

  @Override
  public String toString() {
    return "Student{" +
      "studentId=" + studentId +
      ", name='" + name + '\'' +
      ", phone='" + phone + '\'' +
      ", email='" + email + '\'' +
      ", sex=" + sex +
      ", locked=" + locked +
      ", gmtCreated=" + gmtCreated +
      ", gmtModified=" + gmtModified +
      '}';
  }
}
