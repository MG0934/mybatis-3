/**
 *    Copyright 2009-2019 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.reflection;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.ReflectPermission;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.ibatis.reflection.invoker.AmbiguousMethodInvoker;
import org.apache.ibatis.reflection.invoker.GetFieldInvoker;
import org.apache.ibatis.reflection.invoker.Invoker;
import org.apache.ibatis.reflection.invoker.MethodInvoker;
import org.apache.ibatis.reflection.invoker.SetFieldInvoker;
import org.apache.ibatis.reflection.property.PropertyNamer;

/**
 * This class represents a cached set of class definition information that
 * allows for easy mapping between property names and getter/setter methods.
 *
 * @author Clinton Begin
 */
public class Reflector {
  /**
   * 对应的类
   */
  private final Class<?> type;
  /**
   * 可读属性数组
   */
  private final String[] readablePropertyNames;
  /**
   * 可写属性数组
   */
  private final String[] writablePropertyNames;
  /**
   * 属性对应的settring方法的映射
   *
   * key 为属性名称
   * value 为invoker 对象
   */
  private final Map<String, Invoker> setMethods = new HashMap<>();
  /**
   * 属性对应的getting方法映射
   *
   * key为属性名称
   * value 为invoker 对象
   */
  private final Map<String, Invoker> getMethods = new HashMap<>();
  /**
   * 属性对应的 setting方法的方法参数类型映射 {@link #setMethods}
   *
   * key为属性名称
   * value 为方法属性类型
   */
  private final Map<String, Class<?>> setTypes = new HashMap<>();
  /**
   * 属性对应的getting 方法的返回值类型的映射 {@link #getMethods}
   *
   * key为属性名称
   * value 为方法参数类型
   */
  private final Map<String, Class<?>> getTypes = new HashMap<>();
  /**
   * 默认构造方法
   */
  private Constructor<?> defaultConstructor;
  /**
   * 不区分大小写属性集合
   */
  private Map<String, String> caseInsensitivePropertyMap = new HashMap<>();

  public Reflector(Class<?> clazz) {
    //设置对应的类
    type = clazz;
    //初始化defaultConstructor 默认构造
    addDefaultConstructor(clazz);
    //初始化getMethods和getTypes 通过遍历getting方法
    addGetMethods(clazz);
    //初始化 setMethods 和 setTypes ，通过遍历 setting 方法。
    addSetMethods(clazz);
    //初始化 getMethods + getTypes 和 setMethods + setTypes ，通过遍历 fields 属性。
    addFields(clazz);
    //初始化 readablePropertyNames、writeablePropertyNames、caseInsensitivePropertyMap 属性
    readablePropertyNames = getMethods.keySet().toArray(new String[0]);
    writablePropertyNames = setMethods.keySet().toArray(new String[0]);
    for (String propName : readablePropertyNames) {
      caseInsensitivePropertyMap.put(propName.toUpperCase(Locale.ENGLISH), propName);
    }
    for (String propName : writablePropertyNames) {
      caseInsensitivePropertyMap.put(propName.toUpperCase(Locale.ENGLISH), propName);
    }
  }

  private void addDefaultConstructor(Class<?> clazz) {
    //获取所有构造方法
    Constructor<?>[] constructors = clazz.getDeclaredConstructors();
    //获取无参构造
    Arrays.stream(constructors).filter(constructor -> constructor.getParameterTypes().length == 0)
      .findAny().ifPresent(constructor -> this.defaultConstructor = constructor);
  }

  private void addGetMethods(Class<?> clazz) {
    //属性与getting方法的映射 (解决冲突的集合)
    Map<String, List<Method>> conflictingGetters = new HashMap<>();
    //获得所有方法
    Method[] methods = getClassMethods(clazz);
    //参数等于0并且方法名称以get或者is开头
    Arrays.stream(methods).filter(m -> m.getParameterTypes().length == 0 && PropertyNamer.isGetter(m.getName()))
    //PropertyNamer.methodToProperty(name)获取到属性,并且添加到conflictingGetters
      .forEach(m -> addMethodConflict(conflictingGetters, PropertyNamer.methodToProperty(m.getName()), m));
    // 解决 getting 冲突方法
    resolveGetterConflicts(conflictingGetters);
  }

  /**
   * 解决getting方法冲突
   *
   * @param conflictingGetters
   */
  private void resolveGetterConflicts(Map<String, List<Method>> conflictingGetters) {
    //遍历每个属性，查找最匹配的方法，因为子类可以覆写父类的方法，所以一个属性，可能对应多个getting方法
    for (Entry<String, List<Method>> entry : conflictingGetters.entrySet()) {
      //优胜者 最匹配的方法
      Method winner = null;
      String propName = entry.getKey();
      boolean isAmbiguous = false;

      for (Method candidate : entry.getValue()) {
        //如果winner为空，则candidate(候选人)为最匹配的方法
        if (winner == null) {
          winner = candidate;
          continue;
        }
        //基于返回值类型比较
        Class<?> winnerType = winner.getReturnType();
        Class<?> candidateType = candidate.getReturnType();
        //类型相同
        if (candidateType.equals(winnerType)) {

          if (!boolean.class.equals(candidateType)) {
            isAmbiguous = true;
            break;
            //选择boolean类型is开头的方法
          } else if (candidate.getName().startsWith("is")) {
            winner = candidate;
          }
          //不符合的子类
        } else if (candidateType.isAssignableFrom(winnerType)) {
          //符合选择子类，应为子类可以修改方法返回值，例如父类的一个方法是list
          //则子类可以覆盖该方法为arraylist
          // OK getter type is descendant
        } else if (winnerType.isAssignableFrom(candidateType)) {
          winner = candidate;
        } else {
          isAmbiguous = true;
          break;
        }
      }
      //添加到getMethods和getTypeS中
      addGetMethod(propName, winner, isAmbiguous);
    }
  }

  private void addGetMethod(String name, Method method, boolean isAmbiguous) {
    //判断是否名称合理，并且实例化
    MethodInvoker invoker = isAmbiguous
        ? new AmbiguousMethodInvoker(method, MessageFormat.format(
            "Illegal overloaded getter method with ambiguous type for property ''{0}'' in class ''{1}''. This breaks the JavaBeans specification and can cause unpredictable results.",
            name, method.getDeclaringClass().getName()))
        : new MethodInvoker(method);
    //添加到getMethods中
    getMethods.put(name, invoker);
    //获取返回值类型，添加到getTypes中
    Type returnType = TypeParameterResolver.resolveReturnType(method, type);
    getTypes.put(name, typeToClass(returnType));
  }

  private void addSetMethods(Class<?> clazz) {
    //属性与其seeting方法的映射
    Map<String, List<Method>> conflictingSetters = new HashMap<>();
    //获取所有方法
    Method[] methods = getClassMethods(clazz);
    //获取方面名为set 并且参数为1的
    Arrays.stream(methods).filter(m -> m.getParameterTypes().length == 1 && PropertyNamer.isSetter(m.getName()))
      //获取属性名称
      //添加到conflictingSetters中
      .forEach(m -> addMethodConflict(conflictingSetters, PropertyNamer.methodToProperty(m.getName()), m));
    //解析setting冲突方法
    resolveSetterConflicts(conflictingSetters);
  }

  private void addMethodConflict(Map<String, List<Method>> conflictingMethods, String name, Method method) {
    if (isValidPropertyName(name)) {
      //computeIfAbsent 获取name 如果不存在则创建一个新的arraylist
      List<Method> list = conflictingMethods.computeIfAbsent(name, k -> new ArrayList<>());
      //添加到集合中
      list.add(method);
    }
  }

  private void resolveSetterConflicts(Map<String, List<Method>> conflictingSetters) {
    //遍历每个属性，查找其最匹配的方法。因为子类可以覆写父类的方法，所以一个属性，可能对应多个 setting 方法
    for (String propName : conflictingSetters.keySet()) {
      List<Method> setters = conflictingSetters.get(propName);
      Class<?> getterType = getTypes.get(propName);
      boolean isGetterAmbiguous = getMethods.get(propName) instanceof AmbiguousMethodInvoker;
      boolean isSetterAmbiguous = false;
      Method match = null;
      //遍历属性对应的setting方法
      for (Method setter : setters) {
        //和getterType相同 直接使用
        if (!isGetterAmbiguous && setter.getParameterTypes()[0].equals(getterType)) {
          // should be the best match
          match = setter;
          break;
        }
        if (!isSetterAmbiguous) {
          //选择一个更加匹配的
          match = pickBetterSetter(match, setter, propName);
          isSetterAmbiguous = match == null;
        }
      }
      if (match != null) {
        //添加setMethods和setTypes中
        addSetMethod(propName, match);
      }
    }
  }

  /**
   * 挑选一个最匹配的setter
   *
   * @param setter1
   * @param setter2
   * @param property
   * @return
   */
  private Method pickBetterSetter(Method setter1, Method setter2, String property) {
    if (setter1 == null) {
      return setter2;
    }
    Class<?> paramType1 = setter1.getParameterTypes()[0];
    Class<?> paramType2 = setter2.getParameterTypes()[0];
    //如果2是1的子类 则返回2
    if (paramType1.isAssignableFrom(paramType2)) {
      return setter2;
      //如果1是2的子类 则返回1
    } else if (paramType2.isAssignableFrom(paramType1)) {
      return setter1;
    }
    MethodInvoker invoker = new AmbiguousMethodInvoker(setter1,
        MessageFormat.format(
            "Ambiguous setters defined for property ''{0}'' in class ''{1}'' with types ''{2}'' and ''{3}''.",
            property, setter2.getDeclaringClass().getName(), paramType1.getName(), paramType2.getName()));
    setMethods.put(property, invoker);
    Type[] paramTypes = TypeParameterResolver.resolveParamTypes(setter1, type);
    setTypes.put(property, typeToClass(paramTypes[0]));
    return null;
  }

  private void addSetMethod(String name, Method method) {
    MethodInvoker invoker = new MethodInvoker(method);
    //添加到setMethods中
    setMethods.put(name, invoker);
    Type[] paramTypes = TypeParameterResolver.resolveParamTypes(method, type);
    //添加到setTypes中
    setTypes.put(name, typeToClass(paramTypes[0]));
  }

  private Class<?> typeToClass(Type src) {
    Class<?> result = null;
    //普通类型直接使用
    if (src instanceof Class) {
      result = (Class<?>) src;
      //泛型类型，使用泛型
    } else if (src instanceof ParameterizedType) {
      result = (Class<?>) ((ParameterizedType) src).getRawType();
      //泛型数组，获得具体类
    } else if (src instanceof GenericArrayType) {
      Type componentType = ((GenericArrayType) src).getGenericComponentType();
      //普通类型
      if (componentType instanceof Class) {
        result = Array.newInstance((Class<?>) componentType, 0).getClass();
      } else {
        //递归该方法，返回类
        Class<?> componentClass = typeToClass(componentType);
        result = Array.newInstance(componentClass, 0).getClass();
      }
    }
    //都不符合使用object
    if (result == null) {
      result = Object.class;
    }
    return result;
  }

  /**
   * 是对getMethods和setMethods的补充，
   * 因为某些字段可能不存在set 或者 get 方法
   *
   * @param clazz
   */
  private void addFields(Class<?> clazz) {
    //获取所有的field
    Field[] fields = clazz.getDeclaredFields();
    for (Field field : fields) {
      //filed不存在在setMethods中
      if (!setMethods.containsKey(field.getName())) {
        // issue #379 - removed the check for final because JDK 1.5 allows
        // modification of final fields through reflection (JSR-133). (JGB)
        // pr #16 - final static can only be set by the classloader
        int modifiers = field.getModifiers();
        //filed不是final修饰并且是static类型，设置进setField中
        if (!(Modifier.isFinal(modifiers) && Modifier.isStatic(modifiers))) {
          addSetField(field);
        }
      }
      //getMethods中不包含name，则添加进getField中
      if (!getMethods.containsKey(field.getName())) {
        addGetField(field);
      }
    }
    //递归，处理父类
    if (clazz.getSuperclass() != null) {
      addFields(clazz.getSuperclass());
    }
  }

  private void addSetField(Field field) {
    //判断是合理的属性
    if (isValidPropertyName(field.getName())) {
      //添加到setMethods中
      setMethods.put(field.getName(), new SetFieldInvoker(field));
      Type fieldType = TypeParameterResolver.resolveFieldType(field, type);
      //添加到setTypes中
      setTypes.put(field.getName(), typeToClass(fieldType));
    }
  }

  private void addGetField(Field field) {
    //判断是合理的属性
    if (isValidPropertyName(field.getName())) {
      //添加到getMethods中
      getMethods.put(field.getName(), new GetFieldInvoker(field));
      Type fieldType = TypeParameterResolver.resolveFieldType(field, type);
      //添加到getTypes中
      getTypes.put(field.getName(), typeToClass(fieldType));
    }
  }

  /**
   * 是否有效属性名称
   *
   * @param name
   * @return
   */
  private boolean isValidPropertyName(String name) {
    return !(name.startsWith("$") || "serialVersionUID".equals(name) || "class".equals(name));
  }

  /**
   * This method returns an array containing all methods
   * declared in this class and any superclass.
   * We use this method, instead of the simpler <code>Class.getMethods()</code>,
   * because we want to look for private methods as well.
   *
   * @param clazz The class
   * @return An array containing all methods in this class
   */
  private Method[] getClassMethods(Class<?> clazz) {
    //每个方法签名与该方法的映射
    Map<String, Method> uniqueMethods = new HashMap<>();
    Class<?> currentClass = clazz;
    //循环类，直到取到超类object
    while (currentClass != null && currentClass != Object.class) {
      //记录当前类记录的方法
      addUniqueMethods(uniqueMethods, currentClass.getDeclaredMethods());

      // we also need to look for interface methods -
      // because the class may be abstract
      //记录接口中定义的方法
      Class<?>[] interfaces = currentClass.getInterfaces();
      for (Class<?> anInterface : interfaces) {
        addUniqueMethods(uniqueMethods, anInterface.getMethods());
      }
      //获取父类
      currentClass = currentClass.getSuperclass();
    }
    //转换成method数组返回
    Collection<Method> methods = uniqueMethods.values();

    return methods.toArray(new Method[0]);
  }

  /**
   * 获取签名方法 格式 returnType#方法名:参数名1,参数名2,参数名3 （返回值结构）
   *
   * @param uniqueMethods
   * @param methods
   */
  private void addUniqueMethods(Map<String, Method> uniqueMethods, Method[] methods) {
    for (Method currentMethod : methods) {
      //忽略桥接方法
      if (!currentMethod.isBridge()) {
        //获取方法签名
        String signature = getSignature(currentMethod);
        // check to see if the method is already known
        // if it is known, then an extended class must have
        // overridden a method
        //当签名不存在时进行添加
        if (!uniqueMethods.containsKey(signature)) {
          //添加到uniqueMethods中
          uniqueMethods.put(signature, currentMethod);
        }
      }
    }
  }

  /**
   * 获取签名
   *
   * @param method
   * @return returnType#方法名:参数名1,参数名2,参数名3 （返回值结构）
   */
  private String getSignature(Method method) {
    StringBuilder sb = new StringBuilder();
    //获取返回值类型
    Class<?> returnType = method.getReturnType();
    if (returnType != null) {
      sb.append(returnType.getName()).append('#');
    }
    //获取方法名称
    sb.append(method.getName());
    //获取方法参数
    Class<?>[] parameters = method.getParameterTypes();
    for (int i = 0; i < parameters.length; i++) {
      sb.append(i == 0 ? ':' : ',').append(parameters[i].getName());
    }
    return sb.toString();
  }

  /**
   * Checks whether can control member accessible.
   *
   * 安全校验，是否可以访问
   *
   * @return If can control member accessible, it return {@literal true}
   * @since 3.5.0
   */
  public static boolean canControlMemberAccessible() {
    try {
      SecurityManager securityManager = System.getSecurityManager();
      if (null != securityManager) {
        securityManager.checkPermission(new ReflectPermission("suppressAccessChecks"));
      }
    } catch (SecurityException e) {
      return false;
    }
    return true;
  }

  /**
   * Gets the name of the class the instance provides information for.
   *
   * @return The class name
   */
  public Class<?> getType() {
    return type;
  }

  public Constructor<?> getDefaultConstructor() {
    if (defaultConstructor != null) {
      return defaultConstructor;
    } else {
      throw new ReflectionException("There is no default constructor for " + type);
    }
  }

  public boolean hasDefaultConstructor() {
    return defaultConstructor != null;
  }

  public Invoker getSetInvoker(String propertyName) {
    Invoker method = setMethods.get(propertyName);
    if (method == null) {
      throw new ReflectionException("There is no setter for property named '" + propertyName + "' in '" + type + "'");
    }
    return method;
  }

  public Invoker getGetInvoker(String propertyName) {
    Invoker method = getMethods.get(propertyName);
    if (method == null) {
      throw new ReflectionException("There is no getter for property named '" + propertyName + "' in '" + type + "'");
    }
    return method;
  }

  /**
   * Gets the type for a property setter.
   *
   * @param propertyName - the name of the property
   * @return The Class of the property setter
   */
  public Class<?> getSetterType(String propertyName) {
    Class<?> clazz = setTypes.get(propertyName);
    if (clazz == null) {
      throw new ReflectionException("There is no setter for property named '" + propertyName + "' in '" + type + "'");
    }
    return clazz;
  }

  /**
   * Gets the type for a property getter.
   *
   * @param propertyName - the name of the property
   * @return The Class of the property getter
   */
  public Class<?> getGetterType(String propertyName) {
    Class<?> clazz = getTypes.get(propertyName);
    //如果没有找到type 则抛出异常
    if (clazz == null) {
      throw new ReflectionException("There is no getter for property named '" + propertyName + "' in '" + type + "'");
    }
    return clazz;
  }

  /**
   * Gets an array of the readable properties for an object.
   *
   * @return The array
   */
  public String[] getGetablePropertyNames() {
    return readablePropertyNames;
  }

  /**
   * Gets an array of the writable properties for an object.
   *
   * @return The array
   */
  public String[] getSetablePropertyNames() {
    return writablePropertyNames;
  }

  /**
   * Check to see if a class has a writable property by name.
   *
   * @param propertyName - the name of the property to check
   * @return True if the object has a writable property by the name
   */
  public boolean hasSetter(String propertyName) {
    return setMethods.keySet().contains(propertyName);
  }

  /**
   * Check to see if a class has a readable property by name.
   *
   * @param propertyName - the name of the property to check
   * @return True if the object has a readable property by the name
   */
  public boolean hasGetter(String propertyName) {
    return getMethods.keySet().contains(propertyName);
  }

  public String findPropertyName(String name) {
    return caseInsensitivePropertyMap.get(name.toUpperCase(Locale.ENGLISH));
  }
}
