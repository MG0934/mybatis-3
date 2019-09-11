/**
 *    Copyright 2009-2018 the original author or authors.
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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

import org.apache.ibatis.reflection.invoker.GetFieldInvoker;
import org.apache.ibatis.reflection.invoker.Invoker;
import org.apache.ibatis.reflection.invoker.MethodInvoker;
import org.apache.ibatis.reflection.property.PropertyTokenizer;

/**
 * 类的元数据
 *
 * @author Clinton Begin
 */
public class MetaClass {

  private final ReflectorFactory reflectorFactory;
  private final Reflector reflector;

  /**
   * 构造式private的，所以通过forClass进行访问
   *
   * @param type
   * @param reflectorFactory
   */
  private MetaClass(Class<?> type, ReflectorFactory reflectorFactory) {
    this.reflectorFactory = reflectorFactory;
    this.reflector = reflectorFactory.findForClass(type);
  }

  public static MetaClass forClass(Class<?> type, ReflectorFactory reflectorFactory) {
    return new MetaClass(type, reflectorFactory);
  }

  /**
   * 获取类的元属性
   *
   * @param name
   * @return
   */
  public MetaClass metaClassForProperty(String name) {
    //获取属性的类
    Class<?> propType = reflector.getGetterType(name);
    //创建Metaclass 对象
    return MetaClass.forClass(propType, reflectorFactory);
  }

  /**
   * 获取属性
   *
   * @param name
   * @return
   */
  public String findProperty(String name) {
    //构建属性
    StringBuilder prop = buildProperty(name, new StringBuilder());
    return prop.length() > 0 ? prop.toString() : null;
  }

  /**
   * 获取属性忽略大小写
   *
   * @param name
   * @param useCamelCaseMapping
   * @return
   */
  public String findProperty(String name, boolean useCamelCaseMapping) {
    //下划线转驼峰
    if (useCamelCaseMapping) {
      name = name.replace("_", "");
    }
    //获得属性
    return findProperty(name);
  }

  /**
   * 获取可读属性
   *
   * @return
   */
  public String[] getGetterNames() {
    return reflector.getGetablePropertyNames();
  }

  /**
   * 获取可写属性
   *
   * @return
   */
  public String[] getSetterNames() {
    return reflector.getSetablePropertyNames();
  }

  /**
   * 获取set类型的参数类型
   *
   * @param name
   * @return
   */

  public Class<?> getSetterType(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (prop.hasNext()) {
      MetaClass metaProp = metaClassForProperty(prop.getName());
      return metaProp.getSetterType(prop.getChildren());
    } else {
      return reflector.getSetterType(prop.getName());
    }
  }

  /**
   * 获取get类型的返回值类型
   *
   * @param name
   * @return
   */
  public Class<?> getGetterType(String name) {
    //获取分词器
    PropertyTokenizer prop = new PropertyTokenizer(name);
    //如果有子属性
    if (prop.hasNext()) {
      //获取到metaclass
      MetaClass metaProp = metaClassForProperty(prop);
      //递归获取  children ，获得返回值的类型
      return metaProp.getGetterType(prop.getChildren());
    }
    // issue #506. Resolve the type inside a Collection Object
    // 直接获得返回值的类型
    return getGetterType(prop);
  }

  /**
   * 创建类的指定属性的Metaclass对象
   *
   * @param prop
   * @return
   */
  private MetaClass metaClassForProperty(PropertyTokenizer prop) {
    //调用getterType获取类型
    Class<?> propType = getGetterType(prop);
    //创建MetaClass对象
    return MetaClass.forClass(propType, reflectorFactory);
  }

  /**
   * 获取返回值类型
   *
   * @param prop
   * @return
   */
  private Class<?> getGetterType(PropertyTokenizer prop) {
    //获取返回的类型
    Class<?> type = reflector.getGetterType(prop.getName());
    //如果获取数组某个位置的元素，则获取其泛型，例如list[0].field 那么就会解析list是什么类型
    //这样才好通过该类型，继续获取field
    if (prop.getIndex() != null && Collection.class.isAssignableFrom(type)) {
      //获取返回值类型
      Type returnType = getGenericGetterType(prop.getName());
      //如果是泛型 则进行真正的类型解析
      if (returnType instanceof ParameterizedType) {
        Type[] actualTypeArguments = ((ParameterizedType) returnType).getActualTypeArguments();
        //这里为什么判断1 因为Collection是Collection<T> 至多一个
        if (actualTypeArguments != null && actualTypeArguments.length == 1) {
          returnType = actualTypeArguments[0];
          if (returnType instanceof Class) {
            type = (Class<?>) returnType;
          } else if (returnType instanceof ParameterizedType) {
            type = (Class<?>) ((ParameterizedType) returnType).getRawType();
          }
        }
      }
    }
    return type;
  }

  /**
   * 获取通用返回值类型
   *
   * @param propertyName
   * @return
   */
  private Type getGenericGetterType(String propertyName) {
    try {
      //获得Invoker对象
      Invoker invoker = reflector.getGetInvoker(propertyName);
      //如果是MethodInvoker对象，则说明是getter方法，解析方法返回值类型
      if (invoker instanceof MethodInvoker) {
        Field _method = MethodInvoker.class.getDeclaredField("method");
        _method.setAccessible(true);
        Method method = (Method) _method.get(invoker);
        return TypeParameterResolver.resolveReturnType(method, reflector.getType());
        //如果是 GetFieldInvoker 则说明是属性字段 直接访问
      } else if (invoker instanceof GetFieldInvoker) {
        Field _field = GetFieldInvoker.class.getDeclaredField("field");
        _field.setAccessible(true);
        Field field = (Field) _field.get(invoker);
        return TypeParameterResolver.resolveFieldType(field, reflector.getType());
      }
    } catch (NoSuchFieldException | IllegalAccessException ignored) {
    }
    return null;
  }

  /**
   * 有setter方法
   *
   * @param name
   * @return
   */
  public boolean hasSetter(String name) {
    //分词器
    PropertyTokenizer prop = new PropertyTokenizer(name);
    //是否有子节点
    if (prop.hasNext()) {
      //获取setter
      if (reflector.hasSetter(prop.getName())) {
        //创建Metaclass对象
        MetaClass metaProp = metaClassForProperty(prop.getName());
        //递归判断子表达式 children ，是否有setting 方法
        return metaProp.hasSetter(prop.getChildren());
      } else {
        return false;
      }
      //没有子节点
    } else {
      //获取setter
      return reflector.hasSetter(prop.getName());
    }
  }

  /**
   * 是否有getter方法
   *
   * @param name
   * @return
   */
  public boolean hasGetter(String name) {
    //分词器
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (prop.hasNext()) {
      //判断是否有该属性的 getting 方法
      if (reflector.hasGetter(prop.getName())) {
        //创建 MetaClass 对象
        MetaClass metaProp = metaClassForProperty(prop);
        //递归判断子表达式 children ，是否有 getting 方法
        return metaProp.hasGetter(prop.getChildren());
      } else {
        return false;
      }
      //没有子节点
    } else {
      //判断是否有该属性的 getting 方法
      return reflector.hasGetter(prop.getName());
    }
  }

  /**
   * get 的 invoker 对象
   *
   * @param name
   * @return
   */
  public Invoker getGetInvoker(String name) {
    return reflector.getGetInvoker(name);
  }

  /**
   * set 的 invoker 对象
   *
   * @param name
   * @return
   */
  public Invoker getSetInvoker(String name) {
    return reflector.getSetInvoker(name);
  }

  private StringBuilder buildProperty(String name, StringBuilder builder) {
    //创建分词器对象，对那么进行分词
    PropertyTokenizer prop = new PropertyTokenizer(name);
    //有子表达式
    if (prop.hasNext()) {
      //获取属性名,并且添加到builder中
      String propertyName = reflector.findPropertyName(prop.getName());
      if (propertyName != null) {
        builder.append(propertyName);
        builder.append(".");
        //创建metaclass对象
        MetaClass metaProp = metaClassForProperty(propertyName);
        //递归解析子表达式children 并将结果添加多builder中
        metaProp.buildProperty(prop.getChildren(), builder);
      }
      //没有子表达式
    } else {
      //直接获取属性名（忽略大小写），添加到builder中
      String propertyName = reflector.findPropertyName(name);
      if (propertyName != null) {
        builder.append(propertyName);
      }
    }
    return builder;
  }

  public boolean hasDefaultConstructor() {
    return reflector.hasDefaultConstructor();
  }

}
