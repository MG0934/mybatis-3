package com.practice.reflection.invoker;

import com.practice.reflection.Man;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class MethodTest {
  public static void main(String[] args) throws IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException {
    Class<Man> manClass = Man.class;
    List<Class<?>> constructorArgTypes = new ArrayList<>();

    constructorArgTypes.add(String.class);
    constructorArgTypes.add(String.class);

    Class[] classes = constructorArgTypes.toArray(new Class[constructorArgTypes.size()]);
    Constructor<Man> declaredConstructor = manClass.getDeclaredConstructor(classes);
    String[] args1 = {"mxy","male"};
    Man man = declaredConstructor.newInstance(args1);
    man.getName();
    man.getSex();
  }
}
