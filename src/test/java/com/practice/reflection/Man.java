package com.practice.reflection;

public class Man implements Person{
  @Override
  public void getName() {
    System.out.println("mxy");
  }

  @Override
  public void getSex() {
    System.out.println("male");
  }
}
