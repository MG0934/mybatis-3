package com.practice.reflection;

import org.apache.ibatis.reflection.Reflector;
import org.junit.Test;

public class ReflectorTest {
  @Test
  public void testReflector(){
    Reflector reflector = new Reflector(Man.class);
    System.out.println(reflector);
  }
}
