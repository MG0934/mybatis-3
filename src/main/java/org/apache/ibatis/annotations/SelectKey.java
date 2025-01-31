/**
 *    Copyright 2009-2016 the original author or authors.
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
package org.apache.ibatis.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.ibatis.mapping.StatementType;

/**
 * @author Clinton Begin
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SelectKey {
  /**
   * @return 语句
   */
  String[] statement();
  /**
   * @return Java 对象的属性
   */
  String keyProperty();
  /**
   * @return 数据库的字段
   */
  String keyColumn() default "";
  /**
   * @return 在插入语句执行前，还是执行后
   */
  boolean before();
  /**
   * @return 返回类型
   */
  Class<?> resultType();
  /**
   * @return {@link #statement()} 的类型
   */
  StatementType statementType() default StatementType.PREPARED;
}
