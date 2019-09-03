package com.practice.bean;

public class BlogCustom extends Blog {

  private Auther author;

  public Auther getAuther() {
    return author;
  }

  public void setAuther(Auther auther) {
    this.author = author;
  }

  @Override
  public String toString() {
    return "BlogCustom{" +
      "author=" + author +
      '}';
  }
}
