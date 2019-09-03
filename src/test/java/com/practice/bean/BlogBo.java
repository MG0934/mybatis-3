package com.practice.bean;

public class BlogBo extends Blog{

  private String username;

  private String email;

  private String userId;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  @Override
  public String toString() {
    return "BlogBo{" +
      "username='" + username + '\'' +
      ", email='" + email + '\'' +
      ", userId='" + userId + '\'' +
      '}';
  }
}
