package com.practice.reflection;

public class Man implements Person{

  public Man() {
  }

  private String name;

  private String sex;

  public Man(String name, String sex) {
    this.name = name;
    this.sex = sex;
  }

  @Override
  public void getName() {
    System.out.println(name);
  }

  @Override
  public void getSex() {
    System.out.println(sex);
  }
}
